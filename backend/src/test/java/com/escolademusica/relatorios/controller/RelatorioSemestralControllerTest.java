package com.escolademusica.relatorios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.domain.exception.PeriodoSemRelatoriosException;
import com.escolademusica.relatorios.dto.AprovarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.ContagemRelatoriosResponseDto;
import com.escolademusica.relatorios.dto.RelatorioSemestralResponseDto;
import com.escolademusica.relatorios.dto.SemestreDisponivelDto;
import com.escolademusica.relatorios.usecase.AprovarRelatorioSemestralUseCase;
import com.escolademusica.relatorios.usecase.ContarRelatoriosPeriodoUseCase;
import com.escolademusica.relatorios.usecase.GerarRelatorioSemestralUseCase;
import com.escolademusica.relatorios.usecase.ListarSemestresDisponiveisUseCase;
import com.escolademusica.relatorios.usecase.RevisarRelatorioSemestralUseCase;
import com.fasterxml.jackson.databind.node.NullNode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de fatia (T082) do {@link RelatorioSemestralController} com MockMvc, cobrindo sucesso e os
 * codigos de erro especificos do fluxo semestral: 422 (periodo sem relatorios, FR-014) e 409
 * (aprovacao com versao desatualizada). Filtros de seguranca desabilitados ({@code addFilters =
 * false}) pois o objetivo aqui e validar o contrato HTTP dos endpoints.
 */
@WebMvcTest(controllers = RelatorioSemestralController.class)
@AutoConfigureMockMvc(addFilters = false)
class RelatorioSemestralControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ListarSemestresDisponiveisUseCase listarSemestresDisponiveisUseCase;
  @MockBean private ContarRelatoriosPeriodoUseCase contarRelatoriosPeriodoUseCase;
  @MockBean private GerarRelatorioSemestralUseCase gerarRelatorioSemestralUseCase;
  @MockBean private RevisarRelatorioSemestralUseCase revisarRelatorioSemestralUseCase;
  @MockBean private AprovarRelatorioSemestralUseCase aprovarRelatorioSemestralUseCase;

  private RelatorioSemestralResponseDto respostaPadrao(UUID relatorioId, int versao) {
    return new RelatorioSemestralResponseDto(
        relatorioId,
        "PENDENTE_REVISAO",
        10,
        NullNode.getInstance(),
        NullNode.getInstance(),
        NullNode.getInstance(),
        NullNode.getInstance(),
        NullNode.getInstance(),
        NullNode.getInstance(),
        NullNode.getInstance(),
        NullNode.getInstance(),
        NullNode.getInstance(),
        "Parecer final",
        "/storage/pdfs/relatorio-semestral.pdf",
        versao);
  }

  @Test
  void deveListarSemestresDisponiveis() throws Exception {
    UUID alunoId = UUID.randomUUID();
    when(listarSemestresDisponiveisUseCase.listar(alunoId))
        .thenReturn(
            List.of(
                new SemestreDisponivelDto(
                    "2026-1",
                    "1º semestre 2026",
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 6, 30))));

    mockMvc
        .perform(get("/internal/v1/alunos/{alunoId}/semestres-disponiveis", alunoId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].chave").value("2026-1"));
  }

  @Test
  void deveRetornarContagemDeRelatorios() throws Exception {
    when(contarRelatoriosPeriodoUseCase.contar(any(), eq("2026-1")))
        .thenReturn(new ContagemRelatoriosResponseDto(24));

    mockMvc
        .perform(
            post("/internal/v1/relatorios-semestrais/contagem")
                .contentType("application/json")
                .content(
                    "{\"alunoId\": \"%s\", \"periodoChave\": \"2026-1\"}"
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantidadeRelatoriosAula").value(24));
  }

  @Test
  void deveGerarRelatorioSemestralComSucesso() throws Exception {
    UUID relatorioId = UUID.randomUUID();
    when(gerarRelatorioSemestralUseCase.gerar(any(), any(), eq("2026-1")))
        .thenReturn(respostaPadrao(relatorioId, 1));

    mockMvc
        .perform(
            post("/internal/v1/relatorios-semestrais")
                .contentType("application/json")
                .content(
                    "{\"alunoId\": \"%s\", \"professorId\": \"%s\", \"periodoChave\": \"2026-1\"}"
                        .formatted(UUID.randomUUID(), UUID.randomUUID())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.relatorioId").value(relatorioId.toString()))
        .andExpect(jsonPath("$.status").value("PENDENTE_REVISAO"));
  }

  @Test
  void deveRetornar422QuandoPeriodoSemRelatorios() throws Exception {
    when(gerarRelatorioSemestralUseCase.gerar(any(), any(), eq("2026-1")))
        .thenThrow(new PeriodoSemRelatoriosException("nenhum relatorio no periodo"));

    mockMvc
        .perform(
            post("/internal/v1/relatorios-semestrais")
                .contentType("application/json")
                .content(
                    "{\"alunoId\": \"%s\", \"professorId\": \"%s\", \"periodoChave\": \"2026-1\"}"
                        .formatted(UUID.randomUUID(), UUID.randomUUID())))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.codigo").value("PERIODO_SEM_RELATORIOS"));
  }

  @Test
  void deveAprovarComSucesso() throws Exception {
    UUID relatorioId = UUID.randomUUID();
    OffsetDateTime aprovadoEm = OffsetDateTime.now();
    when(aprovarRelatorioSemestralUseCase.aprovar(eq(relatorioId), anyInt()))
        .thenReturn(new AprovarRelatorioResponseDto(relatorioId, "APROVADO", aprovadoEm));

    mockMvc
        .perform(
            post("/internal/v1/relatorios-semestrais/{id}/aprovar", relatorioId)
                .contentType("application/json")
                .content("{\"versaoConfirmada\": 1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APROVADO"));
  }

  @Test
  void deveRetornar409QuandoVersaoDeAprovacaoDesatualizada() throws Exception {
    UUID relatorioId = UUID.randomUUID();
    when(aprovarRelatorioSemestralUseCase.aprovar(eq(relatorioId), anyInt()))
        .thenThrow(new IllegalStateException("versao desatualizada"));

    mockMvc
        .perform(
            post("/internal/v1/relatorios-semestrais/{id}/aprovar", relatorioId)
                .contentType("application/json")
                .content("{\"versaoConfirmada\": 1}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("CONFLITO"));
  }

  @Test
  void deveRevisarComSucesso() throws Exception {
    UUID relatorioId = UUID.randomUUID();
    when(revisarRelatorioSemestralUseCase.revisar(eq(relatorioId), any()))
        .thenReturn(respostaPadrao(relatorioId, 2));

    mockMvc
        .perform(
            post("/internal/v1/relatorios-semestrais/{id}/revisar", relatorioId)
                .contentType("application/json")
                .content("{\"instrucao\": \"Ajuste o parecer final\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versao").value(2));
  }
}
