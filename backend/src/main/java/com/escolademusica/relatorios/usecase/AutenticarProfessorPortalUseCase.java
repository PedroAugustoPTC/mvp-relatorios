package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.exception.CodigoVinculacaoInvalidoException;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.service.JwtService;
import com.escolademusica.relatorios.service.RateLimiterAutenticacaoWebService;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autentica o professor no portal web pelo mesmo codigo de vinculacao ja usado no Telegram (spec
 * 002, FR-003/FR-004/FR-004a).
 *
 * <p><b>Identidade unica entre canais:</b> o codigo aponta para o mesmo registro de {@link
 * Professor} usado pelo bot — nao existe "conta web" separada. Autenticar aqui e idempotente: usar
 * o mesmo codigo de novo devolve uma sessao para o MESMO professor, nunca cria um segundo.
 *
 * <p><b>Diferenca deliberada em relacao ao {@code VincularContaTelegramUseCase}:</b> aquele zera o
 * codigo apos o uso porque o vinculo Telegram e permanente (a conta do Telegram passa a identificar
 * o professor dali em diante). No portal web nao ha vinculo permanente equivalente — a sessao e um
 * JWT que expira por inatividade (FR-005). Zerar o codigo aqui deixaria o professor sem forma de
 * reautenticar apos a expiracao, dependendo da secretaria a cada 12h. O codigo continua, portanto,
 * valido ate a sua propria data de expiracao ({@code codigoVinculacaoExpiraEm}), que e o que limita
 * a janela de uso.
 *
 * <p>O rate limit e aplicado ANTES da validacao do codigo e a contagem so e zerada em caso de
 * sucesso, para que tentativas automatizadas sejam efetivamente barradas.
 */
@Service
public class AutenticarProfessorPortalUseCase {

  private final ProfessorRepository professorRepository;
  private final JwtService jwtService;
  private final RateLimiterAutenticacaoWebService rateLimiter;

  public AutenticarProfessorPortalUseCase(
      ProfessorRepository professorRepository,
      JwtService jwtService,
      RateLimiterAutenticacaoWebService rateLimiter) {
    this.professorRepository = professorRepository;
    this.jwtService = jwtService;
    this.rateLimiter = rateLimiter;
  }

  /** Sessao emitida para o professor autenticado no portal. */
  public record Resultado(
      String token, OffsetDateTime expiraEm, java.util.UUID professorId, String nomeProfessor) {}

  @Transactional
  public Resultado autenticar(String identificadorOrigem, String codigoVinculacao) {
    rateLimiter.verificarBloqueio(identificadorOrigem);

    OffsetDateTime agora = OffsetDateTime.now();
    Professor professor =
        professorRepository
            .findByCodigoVinculacao(codigoVinculacao)
            .filter(p -> p.codigoVinculacaoValidoEm(agora))
            .filter(Professor::isAtivo)
            .orElse(null);

    if (professor == null) {
      rateLimiter.registrarFalha(identificadorOrigem);
      throw new CodigoVinculacaoInvalidoException("Codigo de vinculacao invalido ou expirado");
    }

    rateLimiter.registrarSucesso(identificadorOrigem);

    JwtService.TokenGerado token = jwtService.gerarTokenProfessor(professor.getId());
    return new Resultado(token.token(), token.expiraEm(), professor.getId(), professor.getNome());
  }
}
