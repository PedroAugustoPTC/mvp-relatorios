package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.exception.AutenticacaoBloqueadaException;
import com.escolademusica.relatorios.domain.exception.CodigoVinculacaoInvalidoException;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.service.JwtService;
import com.escolademusica.relatorios.service.RateLimiterAutenticacaoWebService;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Testes de {@link AutenticarProfessorPortalUseCase} (T055, FR-003/FR-004/FR-004a). */
class AutenticarProfessorPortalUseCaseTest {

  private static final String ORIGEM = "hash-da-origem";
  private static final String CODIGO = "ABC123";

  private ProfessorRepository professorRepository;
  private JwtService jwtService;
  private RateLimiterAutenticacaoWebService rateLimiter;
  private AutenticarProfessorPortalUseCase useCase;

  private final UUID professorId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    professorRepository = Mockito.mock(ProfessorRepository.class);
    jwtService = Mockito.mock(JwtService.class);
    rateLimiter = Mockito.mock(RateLimiterAutenticacaoWebService.class);
    useCase = new AutenticarProfessorPortalUseCase(professorRepository, jwtService, rateLimiter);
  }

  private Professor professorComCodigoValido() {
    Professor professor = new Professor();
    professor.setId(professorId);
    professor.setNome("Ana Souza");
    professor.setAtivo(true);
    professor.setCodigoVinculacao(CODIGO);
    professor.setCodigoVinculacaoExpiraEm(OffsetDateTime.now().plusDays(1));
    return professor;
  }

  private void prepararJwt() {
    when(jwtService.gerarTokenProfessor(professorId))
        .thenReturn(
            new JwtService.TokenGerado("jwt-do-professor", OffsetDateTime.now().plusHours(12)));
  }

  @Test
  void deveEmitirSessaoQuandoOCodigoEValido() {
    when(professorRepository.findByCodigoVinculacao(CODIGO))
        .thenReturn(Optional.of(professorComCodigoValido()));
    prepararJwt();

    AutenticarProfessorPortalUseCase.Resultado resultado = useCase.autenticar(ORIGEM, CODIGO);

    assertThat(resultado.token()).isEqualTo("jwt-do-professor");
    assertThat(resultado.professorId()).isEqualTo(professorId);
    assertThat(resultado.nomeProfessor()).isEqualTo("Ana Souza");
    verify(rateLimiter).registrarSucesso(ORIGEM);
    verify(rateLimiter, never()).registrarFalha(ORIGEM);
  }

  @Test
  void usarOMesmoCodigoDeNovoDeveAutenticarOMesmoProfessorSemCriarOutro() {
    Professor professor = professorComCodigoValido();
    when(professorRepository.findByCodigoVinculacao(CODIGO)).thenReturn(Optional.of(professor));
    prepararJwt();

    AutenticarProfessorPortalUseCase.Resultado primeira = useCase.autenticar(ORIGEM, CODIGO);
    AutenticarProfessorPortalUseCase.Resultado segunda = useCase.autenticar(ORIGEM, CODIGO);

    assertThat(segunda.professorId()).isEqualTo(primeira.professorId());
    // Nenhum professor novo e criado nem alterado — a autenticacao web e apenas leitura + JWT.
    verify(professorRepository, never()).save(Mockito.any());
  }

  @Test
  void deveRecusarCodigoInexistenteEContabilizarATentativa() {
    when(professorRepository.findByCodigoVinculacao("NAOEXISTE")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.autenticar(ORIGEM, "NAOEXISTE"))
        .isInstanceOf(CodigoVinculacaoInvalidoException.class);

    verify(rateLimiter).registrarFalha(ORIGEM);
    verify(rateLimiter, never()).registrarSucesso(ORIGEM);
  }

  @Test
  void deveRecusarCodigoExpirado() {
    Professor professor = professorComCodigoValido();
    professor.setCodigoVinculacaoExpiraEm(OffsetDateTime.now().minusMinutes(1));
    when(professorRepository.findByCodigoVinculacao(CODIGO)).thenReturn(Optional.of(professor));

    assertThatThrownBy(() -> useCase.autenticar(ORIGEM, CODIGO))
        .isInstanceOf(CodigoVinculacaoInvalidoException.class);

    verify(rateLimiter).registrarFalha(ORIGEM);
  }

  @Test
  void deveRecusarProfessorInativo() {
    Professor professor = professorComCodigoValido();
    professor.setAtivo(false);
    when(professorRepository.findByCodigoVinculacao(CODIGO)).thenReturn(Optional.of(professor));

    assertThatThrownBy(() -> useCase.autenticar(ORIGEM, CODIGO))
        .isInstanceOf(CodigoVinculacaoInvalidoException.class);
  }

  @Test
  void deveRecusarSemSequerConsultarOCodigoQuandoAOrigemEstaBloqueada() {
    doThrow(new AutenticacaoBloqueadaException("bloqueado", 600))
        .when(rateLimiter)
        .verificarBloqueio(ORIGEM);

    assertThatThrownBy(() -> useCase.autenticar(ORIGEM, CODIGO))
        .isInstanceOf(AutenticacaoBloqueadaException.class);

    // O bloqueio precisa impedir ate a validacao do codigo: caso contrario, o atacante continuaria
    // conseguindo distinguir codigos validos de invalidos pelo tempo/efeito da resposta.
    verify(professorRepository, never()).findByCodigoVinculacao(Mockito.any());
  }
}
