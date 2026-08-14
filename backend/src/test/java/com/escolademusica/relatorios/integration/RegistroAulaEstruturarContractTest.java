package com.escolademusica.relatorios.integration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.controller.RelatorioAulaController;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
import com.escolademusica.relatorios.usecase.AprovarRelatorioAulaUseCase;
import com.escolademusica.relatorios.usecase.EstruturarRelatorioUseCase;
import com.escolademusica.relatorios.usecase.RegistrarAulaUseCase;
import com.escolademusica.relatorios.usecase.ResponderPerguntaUseCase;
import com.escolademusica.relatorios.usecase.RevisarRelatorioAulaUseCase;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T100 — Testes de contrato (shape JSON, nao regras de negocio) do fluxo de registro de aula de
 * contracts/api-n8n-integration.md, focados em {@code POST
 * /internal/v1/relatorios-aula/{aulaId}/estruturar}: confirma exatamente os campos documentados,
 * incluindo {@code perguntasPendentes} preenchido (com {@code pdfUrl} nulo) e vazio (com {@code
 * pdfUrl} preenchido, ja que o backend gera o PDF quando nao ha pendencias) e o mesmo shape sendo
 * reaproveitado por {@code responder-pergunta} e {@code revisar}.
 */
@WebMvcTest(controllers = RelatorioAulaController.class)
@AutoConfigureMockMvc(addFilters = false)
class RegistroAulaEstruturarContractTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private RegistrarAulaUseCase registrarAulaUseCase;
  @MockBean private EstruturarRelatorioUseCase estruturarRelatorioUseCase;
  @MockBean private ResponderPerguntaUseCase responderPerguntaUseCase;
  @MockBean private RevisarRelatorioAulaUseCase revisarRelatorioAulaUseCase;
  @MockBean private AprovarRelatorioAulaUseCase aprovarRelatorioAulaUseCase;

  @Test
  void estruturarComPerguntasPendentesRetornaTodosOsCamposEPdfUrlNulo() throws Exception {
    UUID aulaId = UUID.randomUUID();
    UUID relatorioId = UUID.randomUUID();
    when(estruturarRelatorioUseCase.estruturar(eq(aulaId)))
        .thenReturn(
            new EstruturarRelatorioResponseDto(
                relatorioId,
                "PENDENTE_REVISAO",
                List.of("Musica X"),
                "Melhora na mao direita",
                List.of("Ritmo na segunda parte"),
                List.of(),
                "",
                List.of("Deseja registrar alguma tarefa para casa?"),
                null,
                1));

    mockMvc
        .perform(post("/internal/v1/relatorios-aula/{aulaId}/estruturar", aulaId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.relatorioId").value(relatorioId.toString()))
        .andExpect(jsonPath("$.status").value("PENDENTE_REVISAO"))
        .andExpect(jsonPath("$.conteudosTrabalhados[0]").value("Musica X"))
        .andExpect(jsonPath("$.evolucao").value("Melhora na mao direita"))
        .andExpect(jsonPath("$.dificuldades[0]").value("Ritmo na segunda parte"))
        .andExpect(jsonPath("$.atividadesPropostas").isArray())
        .andExpect(jsonPath("$.observacoes").value(""))
        .andExpect(jsonPath("$.perguntasPendentes").isArray())
        .andExpect(
            jsonPath("$.perguntasPendentes[0]").value("Deseja registrar alguma tarefa para casa?"))
        .andExpect(jsonPath("$.pdfUrl").value(Matchers.nullValue()))
        .andExpect(jsonPath("$.versao").value(1));
  }

  @Test
  void estruturarSemPerguntasPendentesJaRetornaPdfUrlPreenchido() throws Exception {
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
        .andExpect(jsonPath("$.perguntasPendentes").isEmpty())
        .andExpect(jsonPath("$.pdfUrl").value("/storage/pdfs/relatorio.pdf"));
  }

  @Test
  void responderPerguntaAceitaCampoRespostaERetornaMesmoShapeDeEstruturar() throws Exception {
    UUID relatorioId = UUID.randomUUID();
    when(responderPerguntaUseCase.responder(
            eq(relatorioId), eq("Sim, praticar o exercicio de ritmo.")))
        .thenReturn(
            new EstruturarRelatorioResponseDto(
                relatorioId,
                "PENDENTE_REVISAO",
                List.of("Musica X"),
                "Melhora na mao direita",
                List.of(),
                List.of("Praticar exercicio de ritmo"),
                "",
                List.of(),
                "/storage/pdfs/relatorio.pdf",
                1));

    mockMvc
        .perform(
            post("/internal/v1/relatorios-aula/{relatorioId}/responder-pergunta", relatorioId)
                .contentType("application/json")
                .content("{\"resposta\": \"Sim, praticar o exercicio de ritmo.\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.relatorioId").value(relatorioId.toString()))
        .andExpect(jsonPath("$.perguntasPendentes").isEmpty())
        .andExpect(jsonPath("$.pdfUrl").value("/storage/pdfs/relatorio.pdf"));
  }

  @Test
  void revisarAceitaCampoInstrucaoERetornaVersaoIncrementada() throws Exception {
    UUID relatorioId = UUID.randomUUID();
    when(revisarRelatorioAulaUseCase.revisar(eq(relatorioId), eq("Mude a dificuldade para ritmo")))
        .thenReturn(
            new EstruturarRelatorioResponseDto(
                relatorioId,
                "PENDENTE_REVISAO",
                List.of("Musica X"),
                "Melhora na mao direita",
                List.of("Ritmo"),
                List.of(),
                "",
                List.of(),
                "/storage/pdfs/relatorio-v2.pdf",
                2));

    mockMvc
        .perform(
            post("/internal/v1/relatorios-aula/{relatorioId}/revisar", relatorioId)
                .contentType("application/json")
                .content("{\"instrucao\": \"Mude a dificuldade para ritmo\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versao").value(2))
        .andExpect(jsonPath("$.pdfUrl").value("/storage/pdfs/relatorio-v2.pdf"));
  }
}
