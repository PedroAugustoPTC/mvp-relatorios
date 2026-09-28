package com.escolademusica.relatorios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.domain.ProfessorAluno;
import com.escolademusica.relatorios.domain.VinculoTelegram;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.RascunhoPendenteResponseDto;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.repository.VinculoTelegramRepository;
import com.escolademusica.relatorios.service.RelogioEscola;
import com.escolademusica.relatorios.usecase.DetectarRascunhoPendenteUseCase;
import com.escolademusica.relatorios.usecase.EstruturarRelatorioUseCase;
import com.escolademusica.relatorios.usecase.RegistrarAulaUseCase;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de fatia do {@link TelegramRegistroAulaController}: resolucao de professor/aluno a partir
 * do {@code telegramUserId}, ja que cada mensagem do bot dispara uma execucao isolada do n8n sem
 * memoria da anterior.
 */
@WebMvcTest(controllers = TelegramRegistroAulaController.class)
@AutoConfigureMockMvc(addFilters = false)
class TelegramRegistroAulaControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private VinculoTelegramRepository vinculoTelegramRepository;
  @MockBean private ProfessorAlunoRepository professorAlunoRepository;
  @MockBean private AlunoRepository alunoRepository;
  @MockBean private DetectarRascunhoPendenteUseCase detectarRascunhoPendenteUseCase;
  @MockBean private RegistrarAulaUseCase registrarAulaUseCase;
  @MockBean private EstruturarRelatorioUseCase estruturarRelatorioUseCase;
  @MockBean private RelogioEscola relogioEscola;

  private static final String TELEGRAM_USER_ID = "123456789";

  private VinculoTelegram novoVinculo(UUID professorId) {
    VinculoTelegram vinculo = new VinculoTelegram();
    vinculo.setId(UUID.randomUUID());
    vinculo.setProfessorId(professorId);
    vinculo.setTelegramUserId(TELEGRAM_USER_ID);
    return vinculo;
  }

  private Aluno novoAluno(String nome) {
    Aluno aluno = new Aluno();
    aluno.setId(UUID.randomUUID());
    aluno.setNome(nome);
    return aluno;
  }

  // ---------------------------------------------------------------------------
  // GET /internal/v1/telegram/{telegramUserId}/rascunho-pendente
  // ---------------------------------------------------------------------------

  @Test
  void deveDevolverRascunhoPendenteDoProfessorVinculado() throws Exception {
    UUID professorId = UUID.randomUUID();
    when(vinculoTelegramRepository.findByTelegramUserId(TELEGRAM_USER_ID))
        .thenReturn(Optional.of(novoVinculo(professorId)));
    UUID relatorioId = UUID.randomUUID();
    when(detectarRascunhoPendenteUseCase.detectarPorProfessor(professorId))
        .thenReturn(
            new RascunhoPendenteResponseDto(
                true, "AULA", relatorioId, "RASCUNHO", "TELEGRAM", null));

    mockMvc
        .perform(get("/internal/v1/telegram/{telegramUserId}/rascunho-pendente", TELEGRAM_USER_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.existeRascunho").value(true))
        .andExpect(jsonPath("$.relatorioId").value(relatorioId.toString()));
  }

  @Test
  void deveDevolver404QuandoTelegramUserIdNaoVinculado() throws Exception {
    when(vinculoTelegramRepository.findByTelegramUserId(TELEGRAM_USER_ID))
        .thenReturn(Optional.empty());

    mockMvc
        .perform(get("/internal/v1/telegram/{telegramUserId}/rascunho-pendente", TELEGRAM_USER_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
  }

  // ---------------------------------------------------------------------------
  // POST /internal/v1/telegram/{telegramUserId}/alunos/selecionar
  // ---------------------------------------------------------------------------

  @Test
  void deveSelecionarAlunoPorNomeIgnorandoCaixa() throws Exception {
    UUID professorId = UUID.randomUUID();
    when(vinculoTelegramRepository.findByTelegramUserId(TELEGRAM_USER_ID))
        .thenReturn(Optional.of(novoVinculo(professorId)));
    Aluno aluno = novoAluno("Maria Silva");
    when(professorAlunoRepository.findByProfessorId(professorId))
        .thenReturn(List.of(new ProfessorAluno(professorId, aluno.getId())));
    when(alunoRepository.findById(aluno.getId())).thenReturn(Optional.of(aluno));

    mockMvc
        .perform(
            post("/internal/v1/telegram/{telegramUserId}/alunos/selecionar", TELEGRAM_USER_ID)
                .contentType("application/json")
                .content("{\"nomeAluno\": \"MARIA silva\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.encontrado").value(true))
        .andExpect(jsonPath("$.alunoId").value(aluno.getId().toString()))
        .andExpect(jsonPath("$.nomeAluno").value("Maria Silva"));

    verify(vinculoTelegramRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                v ->
                    aluno.getId().equals(v.getAlunoSelecionadoId())
                        && v.getAlunoSelecionadoEm() != null));
  }

  @Test
  void deveResponderNaoEncontradoSemAlterarEstadoQuandoNomeNaoBate() throws Exception {
    UUID professorId = UUID.randomUUID();
    when(vinculoTelegramRepository.findByTelegramUserId(TELEGRAM_USER_ID))
        .thenReturn(Optional.of(novoVinculo(professorId)));
    when(professorAlunoRepository.findByProfessorId(professorId))
        .thenReturn(List.of(new ProfessorAluno(professorId, UUID.randomUUID())));
    when(alunoRepository.findById(any())).thenReturn(Optional.of(novoAluno("Joao Souza")));

    mockMvc
        .perform(
            post("/internal/v1/telegram/{telegramUserId}/alunos/selecionar", TELEGRAM_USER_ID)
                .contentType("application/json")
                .content("{\"nomeAluno\": \"Nome Que Nao Existe\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.encontrado").value(false))
        .andExpect(jsonPath("$.alunoId").doesNotExist());

    verify(vinculoTelegramRepository, never()).save(any());
  }

  // ---------------------------------------------------------------------------
  // POST /internal/v1/telegram/{telegramUserId}/relatorios-aula/audio
  // ---------------------------------------------------------------------------

  @Test
  void deveRegistrarAulaComOAlunoJaSelecionado() throws Exception {
    UUID professorId = UUID.randomUUID();
    UUID alunoId = UUID.randomUUID();
    UUID aulaId = UUID.randomUUID();
    UUID relatorioId = UUID.randomUUID();
    VinculoTelegram vinculo = novoVinculo(professorId);
    vinculo.setAlunoSelecionadoId(alunoId);
    when(vinculoTelegramRepository.findByTelegramUserId(TELEGRAM_USER_ID))
        .thenReturn(Optional.of(vinculo));
    LocalDate hoje = LocalDate.of(2026, 9, 24);
    when(relogioEscola.hoje()).thenReturn(hoje);
    when(registrarAulaUseCase.registrar(eq(professorId), eq(alunoId), eq(hoje), any(), eq("ogg")))
        .thenReturn(new RegistrarAulaUseCase.Resultado(aulaId, "transcricao"));
    when(estruturarRelatorioUseCase.estruturar(aulaId))
        .thenReturn(
            new EstruturarRelatorioResponseDto(
                relatorioId,
                "PENDENTE_REVISAO",
                List.of(),
                null,
                List.of(),
                List.of(),
                "",
                List.of(),
                "/storage/pdfs/relatorio.pdf",
                1));
    MockMultipartFile audio =
        new MockMultipartFile("audio", "voz.oga", "audio/ogg", "conteudo-fake".getBytes());

    mockMvc
        .perform(
            multipart(
                    "/internal/v1/telegram/{telegramUserId}/relatorios-aula/audio",
                    TELEGRAM_USER_ID)
                .file(audio))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.relatorioId").value(relatorioId.toString()));
  }

  @Test
  void deveDevolver404QuandoNenhumAlunoFoiSelecionadoAinda() throws Exception {
    when(vinculoTelegramRepository.findByTelegramUserId(TELEGRAM_USER_ID))
        .thenReturn(Optional.of(novoVinculo(UUID.randomUUID())));
    MockMultipartFile audio =
        new MockMultipartFile("audio", "voz.oga", "audio/ogg", "conteudo-fake".getBytes());

    mockMvc
        .perform(
            multipart(
                    "/internal/v1/telegram/{telegramUserId}/relatorios-aula/audio",
                    TELEGRAM_USER_ID)
                .file(audio))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
  }
}
