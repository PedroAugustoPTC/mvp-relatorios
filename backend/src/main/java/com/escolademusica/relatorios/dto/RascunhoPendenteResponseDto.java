package com.escolademusica.relatorios.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resposta de {@code GET /api/v1/professor/alunos/{alunoId}/rascunho-pendente} (spec 002, FR-022a).
 *
 * <p>Quando nao ha pendencia, {@code existeRascunho} e {@code false} e os demais campos vem nulos —
 * o frontend usa isso para decidir entre oferecer a retomada ou iniciar um relatorio novo.
 *
 * @param tipo {@code AULA} ou {@code SEMESTRAL}
 * @param canalOrigem canal em que o rascunho foi iniciado ({@code TELEGRAM} ou {@code WEB}) — e o
 *     que permite a retomada cross-channel
 */
public record RascunhoPendenteResponseDto(
    boolean existeRascunho,
    String tipo,
    UUID relatorioId,
    String status,
    String canalOrigem,
    OffsetDateTime atualizadoEm) {

  /** Nenhuma pendencia para este professor+aluno. */
  public static RascunhoPendenteResponseDto vazio() {
    return new RascunhoPendenteResponseDto(false, null, null, null, null, null);
  }
}
