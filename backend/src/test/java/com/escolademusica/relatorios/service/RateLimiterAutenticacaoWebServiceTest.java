package com.escolademusica.relatorios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.TentativaAutenticacaoWeb;
import com.escolademusica.relatorios.domain.exception.AutenticacaoBloqueadaException;
import com.escolademusica.relatorios.repository.TentativaAutenticacaoWebRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes do rate limit de autenticacao do portal (T054, FR-004a): bloqueio apos o limite, liberacao
 * quando o bloqueio vence e reset apos sucesso.
 */
class RateLimiterAutenticacaoWebServiceTest {

  private static final String IDENTIFICADOR = "hash-da-origem";
  private static final int MAX_TENTATIVAS = 5;
  private static final int MINUTOS_BLOQUEIO = 15;

  private TentativaAutenticacaoWebRepository repository;
  private RateLimiterAutenticacaoWebService service;

  @BeforeEach
  void setUp() {
    repository = Mockito.mock(TentativaAutenticacaoWebRepository.class);
    service = new RateLimiterAutenticacaoWebService(repository, MAX_TENTATIVAS, MINUTOS_BLOQUEIO);
    when(repository.findByIdentificador(IDENTIFICADOR)).thenReturn(Optional.empty());
  }

  private TentativaAutenticacaoWeb tentativaCom(int falhas, OffsetDateTime bloqueadoAte) {
    TentativaAutenticacaoWeb tentativa =
        new TentativaAutenticacaoWeb(IDENTIFICADOR, OffsetDateTime.now());
    tentativa.setTentativasIncorretas(falhas);
    tentativa.setBloqueadoAte(bloqueadoAte);
    return tentativa;
  }

  @Test
  void naoDeveBloquearQuandoNaoHaTentativaRegistrada() {
    assertThatCode(() -> service.verificarBloqueio(IDENTIFICADOR)).doesNotThrowAnyException();
  }

  @Test
  void deveBloquearAoAtingirOLimiteDeTentativasIncorretas() {
    TentativaAutenticacaoWeb tentativa = tentativaCom(MAX_TENTATIVAS - 1, null);
    when(repository.findByIdentificador(IDENTIFICADOR)).thenReturn(Optional.of(tentativa));

    service.registrarFalha(IDENTIFICADOR);

    ArgumentCaptor<TentativaAutenticacaoWeb> captor =
        ArgumentCaptor.forClass(TentativaAutenticacaoWeb.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getTentativasIncorretas()).isEqualTo(MAX_TENTATIVAS);
    assertThat(captor.getValue().getBloqueadoAte())
        .isNotNull()
        .isAfter(OffsetDateTime.now().plusMinutes(MINUTOS_BLOQUEIO - 1));
  }

  @Test
  void naoDeveBloquearAntesDeAtingirOLimite() {
    TentativaAutenticacaoWeb tentativa = tentativaCom(1, null);
    when(repository.findByIdentificador(IDENTIFICADOR)).thenReturn(Optional.of(tentativa));

    service.registrarFalha(IDENTIFICADOR);

    assertThat(tentativa.getTentativasIncorretas()).isEqualTo(2);
    assertThat(tentativa.getBloqueadoAte()).isNull();
    assertThatCode(() -> service.verificarBloqueio(IDENTIFICADOR)).doesNotThrowAnyException();
  }

  @Test
  void deveCriarORegistroNaPrimeiraFalhaDeUmaOrigemDesconhecida() {
    service.registrarFalha(IDENTIFICADOR);

    ArgumentCaptor<TentativaAutenticacaoWeb> captor =
        ArgumentCaptor.forClass(TentativaAutenticacaoWeb.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getIdentificador()).isEqualTo(IDENTIFICADOR);
    assertThat(captor.getValue().getTentativasIncorretas()).isEqualTo(1);
  }

  @Test
  void deveRecusarNovasTentativasEnquantoOBloqueioEstiverVigente() {
    when(repository.findByIdentificador(IDENTIFICADOR))
        .thenReturn(
            Optional.of(tentativaCom(MAX_TENTATIVAS, OffsetDateTime.now().plusMinutes(10))));

    assertThatThrownBy(() -> service.verificarBloqueio(IDENTIFICADOR))
        .isInstanceOf(AutenticacaoBloqueadaException.class)
        .satisfies(
            e ->
                assertThat(((AutenticacaoBloqueadaException) e).getRetryAfterSeconds())
                    .isBetween(1L, 600L));
  }

  @Test
  void deveLiberarEZerarAContagemQuandoOBloqueioJaVenceu() {
    TentativaAutenticacaoWeb tentativa =
        tentativaCom(MAX_TENTATIVAS, OffsetDateTime.now().minusMinutes(1));
    when(repository.findByIdentificador(IDENTIFICADOR)).thenReturn(Optional.of(tentativa));

    assertThatCode(() -> service.verificarBloqueio(IDENTIFICADOR)).doesNotThrowAnyException();

    // Sem o reset, a proxima tentativa incorreta rebloquearia de imediato (contador no limite).
    assertThat(tentativa.getTentativasIncorretas()).isZero();
    assertThat(tentativa.getBloqueadoAte()).isNull();
    verify(repository).save(tentativa);
  }

  @Test
  void deveZerarAContagemAposAutenticacaoBemSucedida() {
    TentativaAutenticacaoWeb tentativa = tentativaCom(3, null);
    when(repository.findByIdentificador(IDENTIFICADOR)).thenReturn(Optional.of(tentativa));

    service.registrarSucesso(IDENTIFICADOR);

    assertThat(tentativa.getTentativasIncorretas()).isZero();
    assertThat(tentativa.getBloqueadoAte()).isNull();
    verify(repository).save(tentativa);
  }

  @Test
  void registrarSucessoDeOrigemSemHistoricoNaoDeveGravarNada() {
    service.registrarSucesso(IDENTIFICADOR);

    verify(repository, Mockito.never()).save(any());
  }

  /**
   * Regressao encontrada na validacao end-to-end (T081): com a propagacao padrao ({@code
   * REQUIRED}), o contador entrava na transacao de {@code AutenticarProfessorPortalUseCase} e era
   * desfeito pelo rollback da excecao de codigo invalido — ou seja, a contagem NUNCA acumulava e o
   * bloqueio de FR-004a jamais era atingido. Cada metodo precisa de transacao propria.
   */
  @Test
  void metodosDoRateLimitDevemRodarEmTransacaoPropriaParaSobreviverAoRollbackDoChamador()
      throws NoSuchMethodException {
    for (String metodo : new String[] {"verificarBloqueio", "registrarFalha", "registrarSucesso"}) {
      Transactional anotacao =
          RateLimiterAutenticacaoWebService.class
              .getMethod(metodo, String.class)
              .getAnnotation(Transactional.class);

      assertThat(anotacao).as("@Transactional ausente em %s", metodo).isNotNull();
      assertThat(anotacao.propagation())
          .as("%s precisa de REQUIRES_NEW para nao ser desfeito pelo rollback do chamador", metodo)
          .isEqualTo(Propagation.REQUIRES_NEW);
    }
  }
}
