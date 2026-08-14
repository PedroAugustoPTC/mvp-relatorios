package com.escolademusica.relatorios.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.controller.RelatorioSemestralController;
import com.escolademusica.relatorios.dto.ContagemRelatoriosResponseDto;
import com.escolademusica.relatorios.usecase.AprovarRelatorioSemestralUseCase;
import com.escolademusica.relatorios.usecase.ContarRelatoriosPeriodoUseCase;
import com.escolademusica.relatorios.usecase.GerarRelatorioSemestralUseCase;
import com.escolademusica.relatorios.usecase.ListarSemestresDisponiveisUseCase;
import com.escolademusica.relatorios.usecase.RevisarRelatorioSemestralUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T100 — Testes de contrato (shape JSON) do endpoint {@code POST
 * /internal/v1/relatorios-semestrais/contagem} de contracts/api-n8n-integration.md (FR-012), com
 * atencao especial ao caso de contagem zero (FR-014): o contrato exige {@code Response 200} com
 * {@code { "quantidadeRelatoriosAula": 0 } } — nao um erro — para que o n8n possa informar ausencia
 * de dados ao professor sem prosseguir com a geracao.
 */
@WebMvcTest(controllers = RelatorioSemestralController.class)
@AutoConfigureMockMvc(addFilters = false)
class RelatorioSemestralContagemContractTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ListarSemestresDisponiveisUseCase listarSemestresDisponiveisUseCase;
  @MockBean private ContarRelatoriosPeriodoUseCase contarRelatoriosPeriodoUseCase;
  @MockBean private GerarRelatorioSemestralUseCase gerarRelatorioSemestralUseCase;
  @MockBean private RevisarRelatorioSemestralUseCase revisarRelatorioSemestralUseCase;
  @MockBean private AprovarRelatorioSemestralUseCase aprovarRelatorioSemestralUseCase;

  @Test
  void requisicaoDeContagemAceitaAlunoIdEPeriodoChave() throws Exception {
    UUID alunoId = UUID.randomUUID();
    when(contarRelatoriosPeriodoUseCase.contar(eq(alunoId), eq("2026-1")))
        .thenReturn(new ContagemRelatoriosResponseDto(24));

    mockMvc
        .perform(
            post("/internal/v1/relatorios-semestrais/contagem")
                .contentType("application/json")
                .content("{\"alunoId\": \"%s\", \"periodoChave\": \"2026-1\"}".formatted(alunoId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantidadeRelatoriosAula").value(24))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void contagemZeroRetorna200ComQuantidadeZeroNaoUmErro() throws Exception {
    UUID alunoId = UUID.randomUUID();
    when(contarRelatoriosPeriodoUseCase.contar(eq(alunoId), eq("2026-2")))
        .thenReturn(new ContagemRelatoriosResponseDto(0));

    mockMvc
        .perform(
            post("/internal/v1/relatorios-semestrais/contagem")
                .contentType("application/json")
                .content("{\"alunoId\": \"%s\", \"periodoChave\": \"2026-2\"}".formatted(alunoId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantidadeRelatoriosAula").value(0));
  }

  @Test
  void requisicaoSemAlunoIdERejeitadaComDadosInvalidos() throws Exception {
    mockMvc
        .perform(
            post("/internal/v1/relatorios-semestrais/contagem")
                .contentType("application/json")
                .content("{\"periodoChave\": \"2026-1\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
  }

  @Test
  void gerarComQuantidadeZeroRetorna422PeriodoSemRelatoriosDefesaEmProfundidade() throws Exception {
    when(gerarRelatorioSemestralUseCase.gerar(any(), any(), eq("2026-2")))
        .thenThrow(
            new com.escolademusica.relatorios.domain.exception.PeriodoSemRelatoriosException(
                "periodo sem relatorios de aula"));

    mockMvc
        .perform(
            post("/internal/v1/relatorios-semestrais")
                .contentType("application/json")
                .content(
                    "{\"alunoId\": \"%s\", \"professorId\": \"%s\", \"periodoChave\": \"2026-2\"}"
                        .formatted(UUID.randomUUID(), UUID.randomUUID())))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("PERIODO_SEM_RELATORIOS"));
  }
}
