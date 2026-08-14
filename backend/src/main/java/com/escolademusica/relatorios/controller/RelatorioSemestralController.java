package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.dto.AprovarRelatorioRequestDto;
import com.escolademusica.relatorios.dto.AprovarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.ContagemRelatoriosRequestDto;
import com.escolademusica.relatorios.dto.ContagemRelatoriosResponseDto;
import com.escolademusica.relatorios.dto.GerarRelatorioSemestralRequestDto;
import com.escolademusica.relatorios.dto.RelatorioSemestralResponseDto;
import com.escolademusica.relatorios.dto.RevisarRelatorioRequestDto;
import com.escolademusica.relatorios.dto.SemestreDisponivelDto;
import com.escolademusica.relatorios.usecase.AprovarRelatorioSemestralUseCase;
import com.escolademusica.relatorios.usecase.ContarRelatoriosPeriodoUseCase;
import com.escolademusica.relatorios.usecase.GerarRelatorioSemestralUseCase;
import com.escolademusica.relatorios.usecase.ListarSemestresDisponiveisUseCase;
import com.escolademusica.relatorios.usecase.RevisarRelatorioSemestralUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos (chamados pelo n8n) do fluxo de relatorio semestral (T079, US3): semestres
 * disponiveis -> contagem -> gerar -> revisar (opcional) -> aprovar. Protegidos pela cadeia {@code
 * /internal/v1/**} de {@code SecurityConfig} (header {@code X-N8N-Service-Token}).
 */
@RestController
@Tag(
    name = "n8n - Relatorio semestral",
    description = "Fluxo semestres-disponiveis -> contagem -> gerar -> revisar -> aprovar")
public class RelatorioSemestralController {

  private final ListarSemestresDisponiveisUseCase listarSemestresDisponiveisUseCase;
  private final ContarRelatoriosPeriodoUseCase contarRelatoriosPeriodoUseCase;
  private final GerarRelatorioSemestralUseCase gerarRelatorioSemestralUseCase;
  private final RevisarRelatorioSemestralUseCase revisarRelatorioSemestralUseCase;
  private final AprovarRelatorioSemestralUseCase aprovarRelatorioSemestralUseCase;

  public RelatorioSemestralController(
      ListarSemestresDisponiveisUseCase listarSemestresDisponiveisUseCase,
      ContarRelatoriosPeriodoUseCase contarRelatoriosPeriodoUseCase,
      GerarRelatorioSemestralUseCase gerarRelatorioSemestralUseCase,
      RevisarRelatorioSemestralUseCase revisarRelatorioSemestralUseCase,
      AprovarRelatorioSemestralUseCase aprovarRelatorioSemestralUseCase) {
    this.listarSemestresDisponiveisUseCase = listarSemestresDisponiveisUseCase;
    this.contarRelatoriosPeriodoUseCase = contarRelatoriosPeriodoUseCase;
    this.gerarRelatorioSemestralUseCase = gerarRelatorioSemestralUseCase;
    this.revisarRelatorioSemestralUseCase = revisarRelatorioSemestralUseCase;
    this.aprovarRelatorioSemestralUseCase = aprovarRelatorioSemestralUseCase;
  }

  /** {@code GET /internal/v1/alunos/{alunoId}/semestres-disponiveis} */
  @Operation(summary = "Lista o menu pre-definido de semestres selecionaveis (FR-011)")
  @GetMapping("/internal/v1/alunos/{alunoId}/semestres-disponiveis")
  public List<SemestreDisponivelDto> semestresDisponiveis(@PathVariable UUID alunoId) {
    return listarSemestresDisponiveisUseCase.listar(alunoId);
  }

  /** {@code POST /internal/v1/relatorios-semestrais/contagem} */
  @Operation(
      summary = "Informa a quantidade de relatorios de aula existentes no periodo (FR-012/FR-014)")
  @PostMapping("/internal/v1/relatorios-semestrais/contagem")
  public ContagemRelatoriosResponseDto contagem(
      @Valid @RequestBody ContagemRelatoriosRequestDto corpo) {
    return contarRelatoriosPeriodoUseCase.contar(corpo.alunoId(), corpo.periodoChave());
  }

  /** {@code POST /internal/v1/relatorios-semestrais} */
  @Operation(summary = "Gera o relatorio semestral consolidado via LLM e o PDF (FR-013/FR-013a)")
  @PostMapping("/internal/v1/relatorios-semestrais")
  @ResponseStatus(HttpStatus.CREATED)
  public RelatorioSemestralResponseDto gerar(
      @Valid @RequestBody GerarRelatorioSemestralRequestDto corpo) {
    return gerarRelatorioSemestralUseCase.gerar(
        corpo.alunoId(), corpo.professorId(), corpo.periodoChave());
  }

  /** {@code POST /internal/v1/relatorios-semestrais/{id}/revisar} */
  @Operation(summary = "Aplica uma instrucao de alteracao em linguagem natural (FR-015)")
  @PostMapping("/internal/v1/relatorios-semestrais/{id}/revisar")
  public RelatorioSemestralResponseDto revisar(
      @PathVariable UUID id, @Valid @RequestBody RevisarRelatorioRequestDto corpo) {
    return revisarRelatorioSemestralUseCase.revisar(id, corpo.instrucao());
  }

  /** {@code POST /internal/v1/relatorios-semestrais/{id}/aprovar} */
  @Operation(summary = "Confirma a aprovacao explicita do relatorio semestral (FR-015/FR-016)")
  @PostMapping("/internal/v1/relatorios-semestrais/{id}/aprovar")
  public AprovarRelatorioResponseDto aprovar(
      @PathVariable UUID id, @Valid @RequestBody AprovarRelatorioRequestDto corpo) {
    return aprovarRelatorioSemestralUseCase.aprovar(id, corpo.versaoConfirmada());
  }
}
