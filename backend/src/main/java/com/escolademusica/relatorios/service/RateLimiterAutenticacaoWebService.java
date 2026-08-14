package com.escolademusica.relatorios.service;

import com.escolademusica.relatorios.domain.TentativaAutenticacaoWeb;
import com.escolademusica.relatorios.domain.exception.AutenticacaoBloqueadaException;
import com.escolademusica.relatorios.repository.TentativaAutenticacaoWebRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rate limit das tentativas de autenticacao por codigo de vinculacao no portal web (spec 002,
 * FR-004a): apos {@code WEB_LOGIN_MAX_ATTEMPTS} codigos incorretos consecutivos, novas tentativas
 * daquele identificador sao recusadas por {@code WEB_LOGIN_LOCKOUT_MINUTES}.
 *
 * <p>O estado vive em PostgreSQL (Principio IV — fonte unica de verdade) e nao em memoria, para que
 * um restart do container nao zere o bloqueio de quem esta tentando adivinhar codigos.
 *
 * <p>O {@code identificador} e um hash da origem da requisicao, nunca o IP em claro (Principio V):
 * o objetivo e distinguir origens, nao registrar de onde cada professor acessa.
 *
 * <p><b>Por que todos os metodos usam {@link Propagation#REQUIRES_NEW}:</b> o chamador ({@code
 * AutenticarProfessorPortalUseCase}) e {@code @Transactional} e lanca excecao quando o codigo e
 * invalido — exatamente o caso em que a falha precisa ser contabilizada. Com a propagacao padrao
 * ({@code REQUIRED}) a contagem entraria na transacao do chamador e seria desfeita pelo rollback
 * dessa excecao, zerando o contador a cada tentativa e tornando o bloqueio inalcancavel. Uma
 * transacao propria garante que o registro da tentativa sobreviva ao rollback da autenticacao.
 */
@Service
public class RateLimiterAutenticacaoWebService {

  private final TentativaAutenticacaoWebRepository repository;
  private final int maxTentativas;
  private final int minutosBloqueio;

  public RateLimiterAutenticacaoWebService(
      TentativaAutenticacaoWebRepository repository,
      @Value("${security.web-login.max-attempts:5}") int maxTentativas,
      @Value("${security.web-login.lockout-minutes:15}") int minutosBloqueio) {
    this.repository = repository;
    this.maxTentativas = maxTentativas;
    this.minutosBloqueio = minutosBloqueio;
  }

  /**
   * Recusa a tentativa quando ha bloqueio vigente, ANTES de o codigo sequer ser validado — sem
   * isso, o bloqueio nao impediria a forca bruta, apenas a resposta de sucesso.
   *
   * @throws AutenticacaoBloqueadaException (-> 429) enquanto o bloqueio estiver ativo
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void verificarBloqueio(String identificador) {
    OffsetDateTime agora = OffsetDateTime.now();
    repository
        .findByIdentificador(identificador)
        .ifPresent(
            tentativa -> {
              if (tentativa.estaBloqueadoEm(agora)) {
                long segundosRestantes =
                    Math.max(1, Duration.between(agora, tentativa.getBloqueadoAte()).toSeconds());
                throw new AutenticacaoBloqueadaException(
                    "Muitas tentativas incorretas. Tente novamente em alguns minutos.",
                    segundosRestantes);
              }
              // Bloqueio ja vencido: zera a contagem para que a proxima tentativa incorreta comece
              // do zero, em vez de rebloquear de imediato por o contador ainda estar no limite.
              tentativa.limparBloqueioExpirado(agora);
              repository.save(tentativa);
            });
  }

  /** Contabiliza um codigo incorreto e inicia o bloqueio quando o limite for atingido. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void registrarFalha(String identificador) {
    OffsetDateTime agora = OffsetDateTime.now();
    TentativaAutenticacaoWeb tentativa =
        repository
            .findByIdentificador(identificador)
            .orElseGet(() -> new TentativaAutenticacaoWeb(identificador, agora));
    tentativa.registrarFalha(agora, maxTentativas, minutosBloqueio);
    repository.save(tentativa);
  }

  /** Zera a contagem apos uma autenticacao bem-sucedida. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void registrarSucesso(String identificador) {
    repository
        .findByIdentificador(identificador)
        .ifPresent(
            tentativa -> {
              tentativa.registrarSucesso(OffsetDateTime.now());
              repository.save(tentativa);
            });
  }
}
