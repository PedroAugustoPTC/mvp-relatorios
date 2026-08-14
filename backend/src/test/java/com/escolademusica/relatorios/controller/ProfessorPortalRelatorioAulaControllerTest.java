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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.domain.CanalOrigem;
import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioAula.StatusRelatorioAula;
import com.escolademusica.relatorios.domain.exception.AlunoNaoAssociadoException;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.AprovarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
import com.escolademusica.relatorios.gateway.AudioNaoProcessavelException;
import com.escolademusica.relatorios.mapper.RelatorioAulaMapper;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.service.ControleAcessoProfessorService;
import com.escolademusica.relatorios.usecase.AprovarRelatorioAulaUseCase;
import com.escolademusica.relatorios.usecase.CancelarRelatorioAulaUseCase;
import com.escolademusica.relatorios.usecase.EstruturarRelatorioUseCase;
import com.escolademusica.relatorios.usecase.RegistrarAulaUseCase;
import com.escolademusica.relatorios.usecase.ResponderPerguntaUseCase;
import com.escolademusica.relatorios.usecase.RevisarRelatorioAulaUseCase;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de fatia de {@link ProfessorPortalRelatorioAulaController} (T038), incluindo a politica de
 * nao retencao do audio (T030a, FR-009) e o controle de acesso por professor (T038a).
 */
@WebMvcTest(controllers = ProfessorPortalRelatorioAulaController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfessorPortalRelatorioAulaControllerTest {

  private static final String CONTEUDO_AUDIO = "bytes-do-audio-gravado-no-navegador";

  @Autowired private MockMvc mockMvc;

  @MockBean private ControleAcessoProfessorService controleAcesso;
  @MockBean private RegistrarAulaUseCase registrarAulaUseCase;
  @MockBean private EstruturarRelatorioUseCase estruturarRelatorioUseCase;
  @MockBean private ResponderPerguntaUseCase responderPerguntaUseCase;
  @MockBean private RevisarRelatorioAulaUseCase revisarRelatorioAulaUseCase;
  @MockBean private AprovarRelatorioAulaUseCase aprovarRelatorioAulaUseCase;
  @MockBean private CancelarRelatorioAulaUseCase cancelarRelatorioAulaUseCase;
  @MockBean private RelatorioAulaRepository relatorioAulaRepository;
  @MockBean private RelatorioAulaMapper mapper;

  private final UUID professorId = UUID.randomUUID();
  private final UUID alunoId = UUID.randomUUID();
  private final UUID aulaId = UUID.randomUUID();
  private final UUID relatorioId = UUID.randomUUID();

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

  private MockMultipartFile audio() {
    return new MockMultipartFile("audio", "aula.webm", "audio/webm", CONTEUDO_AUDIO.getBytes());
  }

  private RelatorioAula relatorioPersistido() {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setId(relatorioId);
    relatorio.setAulaId(aulaId);
    relatorio.setProfessorId(professorId);
    relatorio.setAlunoId(alunoId);
    relatorio.setTranscricao("transcricao da aula");
    relatorio.setAtualizadoEm(OffsetDateTime.now());
    return relatorio;
  }

  private void prepararEnvioBemSucedido(List<String> perguntasPendentes, String status) {
    when(registrarAulaUseCase.registrar(any(), any(), any(), any(), any()))
        .thenReturn(new RegistrarAulaUseCase.Resultado(aulaId, "transcricao da aula"));
    when(relatorioAulaRepository.findByAulaId(aulaId))
        .thenReturn(Optional.of(relatorioPersistido()));
    when(estruturarRelatorioUseCase.estruturar(aulaId))
        .thenReturn(
            new EstruturarRelatorioResponseDto(
                relatorioId,
                status,
                List.of("Escala de Sol"),
                "Boa evolucao",
                List.of(),
                List.of(),
                "",
                perguntasPendentes,
                status.equals("PENDENTE_REVISAO") ? "/storage/pdfs/relatorio.pdf" : null,
                1));
  }

  @Test
  void deveAceitarOAudioEDevolver202ComORelatorioCriado() throws Exception {
    prepararEnvioBemSucedido(List.of(), "PENDENTE_REVISAO");

    mockMvc
        .perform(
            multipart("/api/v1/professor/alunos/{alunoId}/relatorios-aula/audio", alunoId)
                .file(audio()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.relatorioId").value(relatorioId.toString()))
        .andExpect(jsonPath("$.status").value("PENDENTE_REVISAO"));
  }

  @Test
  void deveGravarCanalDeOrigemWebNoRelatorioCriadoPeloPortal() throws Exception {
    prepararEnvioBemSucedido(List.of(), "PENDENTE_REVISAO");

    mockMvc
        .perform(
            multipart("/api/v1/professor/alunos/{alunoId}/relatorios-aula/audio", alunoId)
                .file(audio()))
        .andExpect(status().isAccepted());

    ArgumentCaptor<RelatorioAula> captor = ArgumentCaptor.forClass(RelatorioAula.class);
    verify(relatorioAulaRepository).save(captor.capture());
    assertThat(captor.getValue().getCanalOrigem()).isEqualTo(CanalOrigem.WEB);
  }

  @Test
  void deveDevolverAsPerguntasPendentesQuandoFaltamInformacoes() throws Exception {
    prepararEnvioBemSucedido(List.of("Qual peca foi trabalhada?"), "RASCUNHO");

    mockMvc
        .perform(
            multipart("/api/v1/professor/alunos/{alunoId}/relatorios-aula/audio", alunoId)
                .file(audio()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("RASCUNHO"))
        .andExpect(jsonPath("$.perguntasPendentes[0]").value("Qual peca foi trabalhada?"));
  }

  @Test
  void deveRetornar422QuandoOAudioNaoEProcessavel() throws Exception {
    when(registrarAulaUseCase.registrar(any(), any(), any(), any(), any()))
        .thenThrow(new AudioNaoProcessavelException("audio corrompido ou em silencio"));

    mockMvc
        .perform(
            multipart("/api/v1/professor/alunos/{alunoId}/relatorios-aula/audio", alunoId)
                .file(audio()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("AUDIO_NAO_PROCESSAVEL"));

    // Nenhum relatorio chega a ser criado/alterado quando o audio nao pode ser transcrito.
    verify(relatorioAulaRepository, never()).save(any());
  }

  /**
   * T030a (FR-009): o arquivo multipart recebido nao pode sobreviver a chamada de transcricao — nem
   * em disco nem em banco. O audio e entregue como {@code byte[]} ao UseCase de transcricao e o que
   * chega a ser persistido e apenas o texto resultante, nunca o conteudo do arquivo.
   */
  @Test
  void naoDevePersistirOAudioAlemDaChamadaDeTranscricao() throws Exception {
    prepararEnvioBemSucedido(List.of(), "PENDENTE_REVISAO");

    mockMvc
        .perform(
            multipart("/api/v1/professor/alunos/{alunoId}/relatorios-aula/audio", alunoId)
                .file(audio()))
        .andExpect(status().isAccepted());

    ArgumentCaptor<byte[]> audioCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(registrarAulaUseCase)
        .registrar(eq(professorId), eq(alunoId), any(), audioCaptor.capture(), eq("webm"));
    assertThat(new String(audioCaptor.getValue())).isEqualTo(CONTEUDO_AUDIO);

    ArgumentCaptor<RelatorioAula> relatorioCaptor = ArgumentCaptor.forClass(RelatorioAula.class);
    verify(relatorioAulaRepository).save(relatorioCaptor.capture());
    RelatorioAula persistido = relatorioCaptor.getValue();
    assertThat(persistido.getTranscricao()).isEqualTo("transcricao da aula");
    assertThat(persistido.getTranscricao()).doesNotContain(CONTEUDO_AUDIO);
    assertThat(String.valueOf(persistido.getObservacoes())).doesNotContain(CONTEUDO_AUDIO);
    assertThat(persistido.getPdfUrl()).isNull();
  }

  @Test
  void deveUsarADataInformadaPeloProfessorQuandoEnviada() throws Exception {
    prepararEnvioBemSucedido(List.of(), "PENDENTE_REVISAO");

    mockMvc
        .perform(
            multipart("/api/v1/professor/alunos/{alunoId}/relatorios-aula/audio", alunoId)
                .file(audio())
                .param("dataAula", "2026-03-10"))
        .andExpect(status().isAccepted());

    verify(registrarAulaUseCase)
        .registrar(eq(professorId), eq(alunoId), eq(LocalDate.of(2026, 3, 10)), any(), any());
  }

  @Test
  void deveAssumirADataDeHojeQuandoNenhumaDataEInformada() throws Exception {
    prepararEnvioBemSucedido(List.of(), "PENDENTE_REVISAO");

    mockMvc
        .perform(
            multipart("/api/v1/professor/alunos/{alunoId}/relatorios-aula/audio", alunoId)
                .file(audio()))
        .andExpect(status().isAccepted());

    verify(registrarAulaUseCase)
        .registrar(eq(professorId), eq(alunoId), eq(LocalDate.now()), any(), any());
  }

  @Test
  void deveDeduzirOFormatoDoContentTypeQuandoOArquivoNaoTemExtensao() throws Exception {
    prepararEnvioBemSucedido(List.of(), "PENDENTE_REVISAO");
    MockMultipartFile semExtensao =
        new MockMultipartFile("audio", "gravacao", "audio/ogg", CONTEUDO_AUDIO.getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/professor/alunos/{alunoId}/relatorios-aula/audio", alunoId)
                .file(semExtensao))
        .andExpect(status().isAccepted());

    verify(registrarAulaUseCase).registrar(any(), any(), any(), any(), eq("ogg"));
  }

  @Test
  void deveCairNoFormatoPadraoDoNavegadorQuandoNaoHaExtensaoNemContentType() throws Exception {
    prepararEnvioBemSucedido(List.of(), "PENDENTE_REVISAO");
    MockMultipartFile semPistas =
        new MockMultipartFile("audio", "gravacao", null, CONTEUDO_AUDIO.getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/professor/alunos/{alunoId}/relatorios-aula/audio", alunoId)
                .file(semPistas))
        .andExpect(status().isAccepted());

    verify(registrarAulaUseCase).registrar(any(), any(), any(), any(), eq("webm"));
  }

  @Test
  void deveResponder404QuandoORelatorioRecemCriadoNaoEEncontradoParaMarcarOCanal()
      throws Exception {
    when(registrarAulaUseCase.registrar(any(), any(), any(), any(), any()))
        .thenReturn(new RegistrarAulaUseCase.Resultado(aulaId, "transcricao da aula"));
    when(relatorioAulaRepository.findByAulaId(aulaId)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            multipart("/api/v1/professor/alunos/{alunoId}/relatorios-aula/audio", alunoId)
                .file(audio()))
        .andExpect(status().isNotFound());

    verify(estruturarRelatorioUseCase, never()).estruturar(any());
  }

  @Test
  void deveConsultarOEstadoAtualDoRelatorio() throws Exception {
    RelatorioAula relatorio = relatorioPersistido();
    relatorio.setStatus(StatusRelatorioAula.PENDENTE_REVISAO);
    when(controleAcesso.exigirRelatorioAulaDoProfessor(professorId, relatorioId))
        .thenReturn(relatorio);
    when(mapper.paraResponseDto(eq(relatorio), any()))
        .thenReturn(
            new EstruturarRelatorioResponseDto(
                relatorioId,
                "PENDENTE_REVISAO",
                List.of(),
                "",
                List.of(),
                List.of(),
                "",
                List.of(),
                "/storage/pdfs/relatorio.pdf",
                1));

    mockMvc
        .perform(get("/api/v1/professor/relatorios-aula/{relatorioId}", relatorioId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDENTE_REVISAO"))
        .andExpect(jsonPath("$.pdfUrl").value("/storage/pdfs/relatorio.pdf"));
  }

  @Test
  void deveResponderPerguntaPendente() throws Exception {
    when(responderPerguntaUseCase.responder(eq(relatorioId), eq("Trabalhamos a escala de Sol")))
        .thenReturn(
            new EstruturarRelatorioResponseDto(
                relatorioId,
                "PENDENTE_REVISAO",
                List.of(),
                "",
                List.of(),
                List.of(),
                "",
                List.of(),
                "/storage/pdfs/relatorio.pdf",
                1));

    mockMvc
        .perform(
            post("/api/v1/professor/relatorios-aula/{relatorioId}/responder-pergunta", relatorioId)
                .contentType("application/json")
                .content("{\"resposta\": \"Trabalhamos a escala de Sol\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDENTE_REVISAO"));
  }

  @Test
  void deveRevisarGerandoNovaVersao() throws Exception {
    when(revisarRelatorioAulaUseCase.revisar(eq(relatorioId), any()))
        .thenReturn(
            new EstruturarRelatorioResponseDto(
                relatorioId,
                "PENDENTE_REVISAO",
                List.of(),
                "",
                List.of(),
                List.of(),
                "",
                List.of(),
                "/storage/pdfs/relatorio-v2.pdf",
                2));

    mockMvc
        .perform(
            post("/api/v1/professor/relatorios-aula/{relatorioId}/revisar", relatorioId)
                .contentType("application/json")
                .content("{\"instrucao\": \"Ajustar a secao de dificuldades\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versao").value(2));
  }

  @Test
  void deveAprovarAVersaoConfirmada() throws Exception {
    when(aprovarRelatorioAulaUseCase.aprovar(eq(relatorioId), anyInt()))
        .thenReturn(new AprovarRelatorioResponseDto(relatorioId, "APROVADO", OffsetDateTime.now()));

    mockMvc
        .perform(
            post("/api/v1/professor/relatorios-aula/{relatorioId}/aprovar", relatorioId)
                .contentType("application/json")
                .content("{\"versaoConfirmada\": 1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APROVADO"));
  }

  @Test
  void deveRetornar409AoAprovarUmaVersaoDesatualizada() throws Exception {
    when(aprovarRelatorioAulaUseCase.aprovar(eq(relatorioId), anyInt()))
        .thenThrow(new IllegalStateException("versao desatualizada"));

    mockMvc
        .perform(
            post("/api/v1/professor/relatorios-aula/{relatorioId}/aprovar", relatorioId)
                .contentType("application/json")
                .content("{\"versaoConfirmada\": 1}"))
        .andExpect(status().isConflict());
  }

  @Test
  void deveCancelarORascunhoRespondendo204() throws Exception {
    mockMvc
        .perform(post("/api/v1/professor/relatorios-aula/{relatorioId}/cancelar", relatorioId))
        .andExpect(status().isNoContent());

    verify(cancelarRelatorioAulaUseCase).cancelar(relatorioId);
  }

  // ---------------------------------------------------------------------------------------------
  // T038a: nenhum dado de outro professor pode vazar
  // ---------------------------------------------------------------------------------------------

  @Test
  void enviarAudioParaAlunoDeOutroProfessorDeveResponder403SemTranscrever() throws Exception {
    doThrow(new AlunoNaoAssociadoException("Aluno nao esta associado a este professor"))
        .when(controleAcesso)
        .exigirAlunoDoProfessor(professorId, alunoId);

    mockMvc
        .perform(
            multipart("/api/v1/professor/alunos/{alunoId}/relatorios-aula/audio", alunoId)
                .file(audio()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("ALUNO_NAO_ASSOCIADO"));

    verify(registrarAulaUseCase, never()).registrar(any(), any(), any(), any(), any());
  }

  @Test
  void consultarRelatorioDeOutroProfessorDeveResponder404SemExporOConteudo() throws Exception {
    when(controleAcesso.exigirRelatorioAulaDoProfessor(professorId, relatorioId))
        .thenThrow(new RecursoNaoEncontradoException("Relatorio de aula nao encontrado"));

    mockMvc
        .perform(get("/api/v1/professor/relatorios-aula/{relatorioId}", relatorioId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"))
        .andExpect(jsonPath("$.transcricao").doesNotExist())
        .andExpect(jsonPath("$.pdfUrl").doesNotExist());
  }

  @Test
  void aprovarRelatorioDeOutroProfessorDeveResponder404SemAprovar() throws Exception {
    when(controleAcesso.exigirRelatorioAulaDoProfessor(professorId, relatorioId))
        .thenThrow(new RecursoNaoEncontradoException("Relatorio de aula nao encontrado"));

    mockMvc
        .perform(
            post("/api/v1/professor/relatorios-aula/{relatorioId}/aprovar", relatorioId)
                .contentType("application/json")
                .content("{\"versaoConfirmada\": 1}"))
        .andExpect(status().isNotFound());

    verify(aprovarRelatorioAulaUseCase, never()).aprovar(any(), anyInt());
  }

  @Test
  void cancelarRelatorioDeOutroProfessorDeveResponder404SemCancelar() throws Exception {
    when(controleAcesso.exigirRelatorioAulaDoProfessor(professorId, relatorioId))
        .thenThrow(new RecursoNaoEncontradoException("Relatorio de aula nao encontrado"));

    mockMvc
        .perform(post("/api/v1/professor/relatorios-aula/{relatorioId}/cancelar", relatorioId))
        .andExpect(status().isNotFound());

    verify(cancelarRelatorioAulaUseCase, never()).cancelar(any());
  }
}
