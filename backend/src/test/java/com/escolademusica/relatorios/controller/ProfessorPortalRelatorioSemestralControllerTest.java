package com.escolademusica.relatorios.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.domain.CanalOrigem;
import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.exception.AlunoNaoAssociadoException;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.AprovarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.ContagemRelatoriosResponseDto;
import com.escolademusica.relatorios.dto.RelatorioSemestralResponseDto;
import com.escolademusica.relatorios.dto.SemestreDisponivelDto;
import com.escolademusica.relatorios.mapper.RelatorioSemestralMapper;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import com.escolademusica.relatorios.service.ControleAcessoProfessorService;
import com.escolademusica.relatorios.usecase.AprovarRelatorioSemestralUseCase;
import com.escolademusica.relatorios.usecase.CancelarRelatorioSemestralUseCase;
import com.escolademusica.relatorios.usecase.ContarRelatoriosPeriodoUseCase;
import com.escolademusica.relatorios.usecase.GerarRelatorioSemestralUseCase;
import com.escolademusica.relatorios.usecase.ListarSemestresDisponiveisUseCase;
import com.escolademusica.relatorios.usecase.RevisarRelatorioSemestralUseCase;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de fatia de {@link ProfessorPortalRelatorioSemestralController} (T069), incluindo o 409
 * quando nao ha relatorios no periodo e o controle de acesso por professor (T069a).
 */
@WebMvcTest(controllers = ProfessorPortalRelatorioSemestralController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfessorPortalRelatorioSemestralControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ControleAcessoProfessorService controleAcesso;
  @MockBean private ListarSemestresDisponiveisUseCase listarSemestresDisponiveisUseCase;
  @MockBean private ContarRelatoriosPeriodoUseCase contarRelatoriosPeriodoUseCase;
  @MockBean private GerarRelatorioSemestralUseCase gerarRelatorioSemestralUseCase;
  @MockBean private RevisarRelatorioSemestralUseCase revisarRelatorioSemestralUseCase;
  @MockBean private AprovarRelatorioSemestralUseCase aprovarRelatorioSemestralUseCase;
  @MockBean private CancelarRelatorioSemestralUseCase cancelarRelatorioSemestralUseCase;
  @MockBean private RelatorioSemestralRepository relatorioSemestralRepository;
  @MockBean private RelatorioSemestralMapper mapper;

  private final UUID professorId = UUID.randomUUID();
  private final UUID alunoId = UUID.randomUUID();
  private final UUID relatorioId = UUID.randomUUID();

  private static final String CORPO_SEMESTRE = "{\"semestre\": \"2026-1\"}";

  @BeforeEach
  void autenticarProfessor() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                professorId, null, List.of(new SimpleGrantedAuthority("ROLE_PROFESSOR"))));
  }

  @AfterEach
  void limparContexto() {
    SecurityContextHolder.clearContext();
  }

  private RelatorioSemestralResponseDto respostaGerada(String status, int versao) {
    return new RelatorioSemestralResponseDto(
        relatorioId,
        status,
        12,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "Parecer final",
        "/storage/pdfs/semestral.pdf",
        versao);
  }

  @Test
  void deveListarOsSemestresDisponiveisDoAluno() throws Exception {
    when(listarSemestresDisponiveisUseCase.listar(alunoId))
        .thenReturn(
            List.of(
                new SemestreDisponivelDto(
                    "2026-1",
                    "1º semestre 2026",
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 6, 30))));

    mockMvc
        .perform(get("/api/v1/professor/alunos/{alunoId}/semestres-disponiveis", alunoId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].chave").value("2026-1"))
        .andExpect(jsonPath("$[0].rotulo").value("1º semestre 2026"));
  }

  @Test
  void deveGerarORelatorioInformandoQuantosRelatoriosDeAulaForamConsiderados() throws Exception {
    when(contarRelatoriosPeriodoUseCase.contar(alunoId, "2026-1"))
        .thenReturn(new ContagemRelatoriosResponseDto(12));
    when(gerarRelatorioSemestralUseCase.gerar(alunoId, professorId, "2026-1"))
        .thenReturn(respostaGerada("PENDENTE_REVISAO", 1));
    when(relatorioSemestralRepository.findById(relatorioId))
        .thenReturn(Optional.of(new RelatorioSemestral()));

    mockMvc
        .perform(
            post("/api/v1/professor/alunos/{alunoId}/relatorios-semestrais", alunoId)
                .contentType("application/json")
                .content(CORPO_SEMESTRE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantidadeRelatoriosEncontrados").value(12))
        .andExpect(jsonPath("$.relatorioSemestralId").value(relatorioId.toString()))
        .andExpect(jsonPath("$.pdfUrl").value("/storage/pdfs/semestral.pdf"));
  }

  @Test
  void deveGravarCanalDeOrigemWebNoRelatorioGeradoPeloPortal() throws Exception {
    when(contarRelatoriosPeriodoUseCase.contar(alunoId, "2026-1"))
        .thenReturn(new ContagemRelatoriosResponseDto(3));
    when(gerarRelatorioSemestralUseCase.gerar(alunoId, professorId, "2026-1"))
        .thenReturn(respostaGerada("PENDENTE_REVISAO", 1));
    when(relatorioSemestralRepository.findById(relatorioId))
        .thenReturn(Optional.of(new RelatorioSemestral()));

    mockMvc
        .perform(
            post("/api/v1/professor/alunos/{alunoId}/relatorios-semestrais", alunoId)
                .contentType("application/json")
                .content(CORPO_SEMESTRE))
        .andExpect(status().isOk());

    ArgumentCaptor<RelatorioSemestral> captor = ArgumentCaptor.forClass(RelatorioSemestral.class);
    verify(relatorioSemestralRepository).save(captor.capture());
    assertThat(captor.getValue().getCanalOrigem()).isEqualTo(CanalOrigem.WEB);
  }

  @Test
  void deveResponder409SemGerarNadaQuandoNaoHaRelatoriosNoPeriodo() throws Exception {
    when(contarRelatoriosPeriodoUseCase.contar(alunoId, "2026-1"))
        .thenReturn(new ContagemRelatoriosResponseDto(0));

    mockMvc
        .perform(
            post("/api/v1/professor/alunos/{alunoId}/relatorios-semestrais", alunoId)
                .contentType("application/json")
                .content(CORPO_SEMESTRE))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("SEM_DADOS_NO_PERIODO"));

    // Nem relatorio, nem PDF, nem chamada ao LLM (FR-019).
    verify(gerarRelatorioSemestralUseCase, never()).gerar(any(), any(), any());
  }

  @Test
  void deveConsultarOEstadoAtualDoRelatorioSemestral() throws Exception {
    RelatorioSemestral relatorio = new RelatorioSemestral();
    when(controleAcesso.exigirRelatorioSemestralDoProfessor(professorId, relatorioId))
        .thenReturn(relatorio);
    when(mapper.paraResponseDto(relatorio)).thenReturn(respostaGerada("PENDENTE_REVISAO", 1));

    mockMvc
        .perform(get("/api/v1/professor/relatorios-semestrais/{relatorioId}", relatorioId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDENTE_REVISAO"))
        .andExpect(jsonPath("$.parecerFinal").value("Parecer final"));
  }

  @Test
  void deveRevisarGerandoNovaVersao() throws Exception {
    when(revisarRelatorioSemestralUseCase.revisar(eq(relatorioId), any()))
        .thenReturn(respostaGerada("PENDENTE_REVISAO", 2));

    mockMvc
        .perform(
            post("/api/v1/professor/relatorios-semestrais/{relatorioId}/revisar", relatorioId)
                .contentType("application/json")
                .content("{\"instrucao\": \"Detalhar mais a evolução técnica\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versao").value(2));
  }

  @Test
  void deveAprovarAVersaoConfirmada() throws Exception {
    when(aprovarRelatorioSemestralUseCase.aprovar(eq(relatorioId), anyInt()))
        .thenReturn(new AprovarRelatorioResponseDto(relatorioId, "APROVADO", OffsetDateTime.now()));

    mockMvc
        .perform(
            post("/api/v1/professor/relatorios-semestrais/{relatorioId}/aprovar", relatorioId)
                .contentType("application/json")
                .content("{\"versaoConfirmada\": 1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APROVADO"));
  }

  @Test
  void deveCancelarRespondendo204() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/professor/relatorios-semestrais/{relatorioId}/cancelar", relatorioId))
        .andExpect(status().isNoContent());

    verify(cancelarRelatorioSemestralUseCase).cancelar(relatorioId);
  }

  // ---------------------------------------------------------------------------------------------
  // T069a: nenhum dado de outro professor pode vazar
  // ---------------------------------------------------------------------------------------------

  @Test
  void semestresDisponiveisDeAlunoDeOutroProfessorDeveResponder403() throws Exception {
    doThrow(new AlunoNaoAssociadoException("Aluno nao esta associado a este professor"))
        .when(controleAcesso)
        .exigirAlunoDoProfessor(professorId, alunoId);

    mockMvc
        .perform(get("/api/v1/professor/alunos/{alunoId}/semestres-disponiveis", alunoId))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("ALUNO_NAO_ASSOCIADO"));

    verify(listarSemestresDisponiveisUseCase, never()).listar(any());
  }

  @Test
  void gerarRelatorioParaAlunoDeOutroProfessorDeveResponder403SemConsolidar() throws Exception {
    doThrow(new AlunoNaoAssociadoException("Aluno nao esta associado a este professor"))
        .when(controleAcesso)
        .exigirAlunoDoProfessor(professorId, alunoId);

    mockMvc
        .perform(
            post("/api/v1/professor/alunos/{alunoId}/relatorios-semestrais", alunoId)
                .contentType("application/json")
                .content(CORPO_SEMESTRE))
        .andExpect(status().isForbidden());

    verify(gerarRelatorioSemestralUseCase, never()).gerar(any(), any(), any());
  }

  @Test
  void consultarRelatorioDeOutroProfessorDeveResponder404SemExporOConteudo() throws Exception {
    when(controleAcesso.exigirRelatorioSemestralDoProfessor(professorId, relatorioId))
        .thenThrow(new RecursoNaoEncontradoException("Relatorio semestral nao encontrado"));

    mockMvc
        .perform(get("/api/v1/professor/relatorios-semestrais/{relatorioId}", relatorioId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"))
        .andExpect(jsonPath("$.parecerFinal").doesNotExist())
        .andExpect(jsonPath("$.pdfUrl").doesNotExist());
  }
}
