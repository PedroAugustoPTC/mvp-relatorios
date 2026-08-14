package com.escolademusica.relatorios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.ProfessorAluno;
import com.escolademusica.relatorios.domain.exception.AlunoNaoAssociadoException;
import com.escolademusica.relatorios.dto.AlunoResumoDto;
import com.escolademusica.relatorios.dto.HistoricoAlunoResponseDto;
import com.escolademusica.relatorios.dto.ItemHistoricoDto;
import com.escolademusica.relatorios.dto.RascunhoPendenteResponseDto;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.service.ControleAcessoProfessorService;
import com.escolademusica.relatorios.service.JwtService;
import com.escolademusica.relatorios.usecase.ConsultarHistoricoUseCase;
import com.escolademusica.relatorios.usecase.DetectarRascunhoPendenteUseCase;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de fatia de {@link ProfessorPortalController} (T038/T038a).
 *
 * <p>Os filtros de seguranca sao desabilitados ({@code addFilters = false}) — a autenticacao em si
 * ja e coberta por {@code ProfessorJwtAuthenticationFilterTest}. Aqui o que importa e que o
 * controller use o {@code professorId} do principal (e nunca um id vindo da URL) e que o controle
 * de acesso por aluno seja aplicado; o principal e colocado direto no {@code
 * SecurityContextHolder}.
 */
@WebMvcTest(controllers = ProfessorPortalController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfessorPortalControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ProfessorRepository professorRepository;
  @MockBean private ProfessorAlunoRepository professorAlunoRepository;
  @MockBean private AlunoRepository alunoRepository;
  @MockBean private JwtService jwtService;
  @MockBean private ControleAcessoProfessorService controleAcesso;
  @MockBean private DetectarRascunhoPendenteUseCase detectarRascunhoPendenteUseCase;
  @MockBean private ConsultarHistoricoUseCase consultarHistoricoUseCase;

  private final UUID professorId = UUID.randomUUID();
  private final UUID alunoId = UUID.randomUUID();

  @BeforeEach
  void autenticarProfessor() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                professorId, null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR"))));
    when(jwtService.getExpiracaoProfessor()).thenReturn(Duration.ofHours(12));
  }

  @AfterEach
  void limparContexto() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void meDeveRetornarODadosDoProfessorAutenticadoEAExpiracaoDaSessao() throws Exception {
    Professor professor = new Professor();
    professor.setId(professorId);
    professor.setNome("Ana Souza");
    when(professorRepository.findById(professorId)).thenReturn(Optional.of(professor));

    mockMvc
        .perform(get("/api/v1/professor/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(professorId.toString()))
        .andExpect(jsonPath("$.nome").value("Ana Souza"))
        .andExpect(jsonPath("$.sessaoExpiraEm").isNotEmpty());
  }

  @Test
  void meDeveRetornar404QuandoOProfessorDoTokenNaoExisteMais() throws Exception {
    when(professorRepository.findById(professorId)).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/v1/professor/me"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
  }

  @Test
  void deveListarSomenteOsAlunosDoProfessorAutenticado() throws Exception {
    ProfessorAluno vinculo = new ProfessorAluno();
    vinculo.setProfessorId(professorId);
    vinculo.setAlunoId(alunoId);
    Aluno aluno = new Aluno();
    aluno.setId(alunoId);
    aluno.setNome("Arthur Caldas");
    when(professorAlunoRepository.findByProfessorId(professorId)).thenReturn(List.of(vinculo));
    when(alunoRepository.findById(alunoId)).thenReturn(Optional.of(aluno));

    mockMvc
        .perform(get("/api/v1/professor/alunos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(alunoId.toString()))
        .andExpect(jsonPath("$[0].nome").value("Arthur Caldas"))
        .andExpect(jsonPath("$[1]").doesNotExist());
  }

  @Test
  void deveIgnorarVinculoCujoAlunoNaoExisteMais() throws Exception {
    ProfessorAluno vinculo = new ProfessorAluno();
    vinculo.setProfessorId(professorId);
    vinculo.setAlunoId(alunoId);
    when(professorAlunoRepository.findByProfessorId(professorId)).thenReturn(List.of(vinculo));
    when(alunoRepository.findById(alunoId)).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/v1/professor/alunos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").doesNotExist());
  }

  @Test
  void deveRetornarRascunhoPendenteDetectadoEmOutroCanal() throws Exception {
    UUID relatorioId = UUID.randomUUID();
    when(detectarRascunhoPendenteUseCase.detectar(professorId, alunoId))
        .thenReturn(
            new RascunhoPendenteResponseDto(
                true, "AULA", relatorioId, "RASCUNHO", "TELEGRAM", OffsetDateTime.now()));

    mockMvc
        .perform(get("/api/v1/professor/alunos/{alunoId}/rascunho-pendente", alunoId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.existeRascunho").value(true))
        .andExpect(jsonPath("$.tipo").value("AULA"))
        .andExpect(jsonPath("$.canalOrigem").value("TELEGRAM"))
        .andExpect(jsonPath("$.relatorioId").value(relatorioId.toString()));
  }

  @Test
  void deveRetornarHistoricoUnificadoDoAluno() throws Exception {
    when(consultarHistoricoUseCase.executar(eq(alunoId), eq(professorId)))
        .thenReturn(
            new HistoricoAlunoResponseDto(
                new AlunoResumoDto(alunoId, "Arthur Caldas"),
                List.of(
                    ItemHistoricoDto.deAula(
                        UUID.randomUUID(),
                        LocalDate.of(2026, 3, 10),
                        "APROVADO",
                        "/storage/pdfs/a.pdf",
                        OffsetDateTime.now()))));

    mockMvc
        .perform(get("/api/v1/professor/alunos/{alunoId}/historico", alunoId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aluno.nome").value("Arthur Caldas"))
        .andExpect(jsonPath("$.relatorios[0].tipo").value("AULA"));
  }

  // ---------------------------------------------------------------------------------------------
  // T038a: um professor jamais acessa dados de aluno que nao e dele
  // ---------------------------------------------------------------------------------------------

  @Test
  void rascunhoPendenteDeAlunoDeOutroProfessorDeveResponder403SemConsultarOUseCase()
      throws Exception {
    doThrow(new AlunoNaoAssociadoException("Aluno nao esta associado a este professor"))
        .when(controleAcesso)
        .exigirAlunoDoProfessor(professorId, alunoId);

    mockMvc
        .perform(get("/api/v1/professor/alunos/{alunoId}/rascunho-pendente", alunoId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("ALUNO_NAO_ASSOCIADO"))
        .andExpect(jsonPath("$.relatorioId").doesNotExist());

    org.mockito.Mockito.verify(detectarRascunhoPendenteUseCase, org.mockito.Mockito.never())
        .detectar(any(), any());
  }

  @Test
  void historicoDeAlunoDeOutroProfessorDeveResponder403SemExporOsItens() throws Exception {
    when(consultarHistoricoUseCase.executar(eq(alunoId), eq(professorId)))
        .thenThrow(new AlunoNaoAssociadoException("Aluno nao esta associado a este professor"));

    mockMvc
        .perform(get("/api/v1/professor/alunos/{alunoId}/historico", alunoId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("ALUNO_NAO_ASSOCIADO"))
        .andExpect(jsonPath("$.relatorios").doesNotExist());
  }
}
