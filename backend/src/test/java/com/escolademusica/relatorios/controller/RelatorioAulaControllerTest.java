package com.escolademusica.relatorios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.dto.AprovarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
import com.escolademusica.relatorios.gateway.AudioNaoProcessavelException;
import com.escolademusica.relatorios.usecase.AprovarRelatorioAulaUseCase;
import com.escolademusica.relatorios.usecase.EstruturarRelatorioUseCase;
import com.escolademusica.relatorios.usecase.RegistrarAulaUseCase;
import com.escolademusica.relatorios.usecase.ResponderPerguntaUseCase;
import com.escolademusica.relatorios.usecase.RevisarRelatorioAulaUseCase;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de fatia (T052) do {@link RelatorioAulaController} com MockMvc, cobrindo sucesso, 422
 * (audio nao processavel) e 409 (aprovacao com versao desatualizada). Filtros de seguranca sao
 * desabilitados ({@code addFilters = false}) pois o objetivo aqui e validar o contrato HTTP dos
 * endpoints, nao a autenticacao (ja coberta por SecurityConfig separadamente).
 */
@WebMvcTest(controllers = RelatorioAulaController.class)
@AutoConfigureMockMvc(addFilters = false)
class RelatorioAulaControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private RegistrarAulaUseCase registrarAulaUseCase;
  @MockBean private EstruturarRelatorioUseCase estruturarRelatorioUseCase;
  @MockBean private ResponderPerguntaUseCase responderPerguntaUseCase;
  @MockBean private RevisarRelatorioAulaUseCase revisarRelatorioAulaUseCase;
  @MockBean private AprovarRelatorioAulaUseCase aprovarRelatorioAulaUseCase;

  @Test
  void deveTranscreverComSucesso() throws Exception {
    UUID aulaId = UUID.randomUUID();
    MockMultipartFile audio =
        new MockMultipartFile("audio", "aula.ogg", "audio/ogg", "conteudo-fake".getBytes());
    when(registrarAulaUseCase.registrar(any(), any(), any(), any(), any()))
        .thenReturn(new RegistrarAulaUseCase.Resultado(aulaId, "transcricao da aula"));

    mockMvc
        .perform(
            multipart("/internal/v1/relatorios-aula/transcrever")
                .file(audio)
                .param("alunoId", UUID.randomUUID().toString())
                .param("professorId", UUID.randomUUID().toString())
                .param("dataAula", "2026-08-13"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aulaId").value(aulaId.toString()))
        .andExpect(jsonPath("$.transcricao").value("transcricao da aula"));
  }

  @Test
  void deveRetornar422QuandoAudioNaoProcessavel() throws Exception {
    MockMultipartFile audio = new MockMultipartFile("audio", "aula.ogg", "audio/ogg", new byte[0]);
    when(registrarAulaUseCase.registrar(any(), any(), any(), any(), any()))
        .thenThrow(new AudioNaoProcessavelException("audio corrompido"));

    mockMvc
        .perform(
            multipart("/internal/v1/relatorios-aula/transcrever")
                .file(audio)
                .param("alunoId", UUID.randomUUID().toString())
                .param("professorId", UUID.randomUUID().toString())
                .param("dataAula", "2026-08-13"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("AUDIO_NAO_PROCESSAVEL"));
  }

  @Test
  void deveEstruturarRelatorioComSucesso() throws Exception {
    UUID aulaId = UUID.randomUUID();
    UUID relatorioId = UUID.randomUUID();
    when(estruturarRelatorioUseCase.estruturar(eq(aulaId)))
        .thenReturn(
            new EstruturarRelatorioResponseDto(
                relatorioId,
                "PENDENTE_REVISAO",
                List.of("Musica X"),
                "Melhora na mao direita",
                List.of(),
                List.of(),
                "",
                List.of(),
                "/storage/pdfs/relatorio.pdf",
                1));

    mockMvc
        .perform(post("/internal/v1/relatorios-aula/{aulaId}/estruturar", aulaId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.relatorioId").value(relatorioId.toString()))
        .andExpect(jsonPath("$.status").value("PENDENTE_REVISAO"))
        .andExpect(jsonPath("$.pdfUrl").value("/storage/pdfs/relatorio.pdf"));
  }

  @Test
  void deveAprovarComSucesso() throws Exception {
    UUID relatorioId = UUID.randomUUID();
    OffsetDateTime aprovadoEm = OffsetDateTime.now();
    when(aprovarRelatorioAulaUseCase.aprovar(eq(relatorioId), anyInt()))
        .thenReturn(new AprovarRelatorioResponseDto(relatorioId, "APROVADO", aprovadoEm));

    mockMvc
        .perform(
            post("/internal/v1/relatorios-aula/{relatorioId}/aprovar", relatorioId)
                .contentType("application/json")
                .content("{\"versaoConfirmada\": 1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APROVADO"));
  }

  @Test
  void deveRetornar409QuandoVersaoDeAprovacaoDesatualizada() throws Exception {
    UUID relatorioId = UUID.randomUUID();
    when(aprovarRelatorioAulaUseCase.aprovar(eq(relatorioId), anyInt()))
        .thenThrow(new IllegalStateException("versao desatualizada"));

    mockMvc
        .perform(
            post("/internal/v1/relatorios-aula/{relatorioId}/aprovar", relatorioId)
                .contentType("application/json")
                .content("{\"versaoConfirmada\": 1}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("CONFLITO"));
  }
}
