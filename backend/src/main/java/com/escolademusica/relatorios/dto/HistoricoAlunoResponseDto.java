package com.escolademusica.relatorios.dto;

import java.util.List;

/**
 * Resposta de {@code GET /api/v1/alunos/{id}/historico} e {@code GET
 * /internal/v1/professores/{professorId}/alunos/{alunoId}/historico} (US4,
 * contracts/api-web-admin.md, contracts/api-n8n-integration.md).
 */
public record HistoricoAlunoResponseDto(AlunoResumoDto aluno, List<ItemHistoricoDto> relatorios) {}
