package com.escolademusica.relatorios.dto;

import java.util.UUID;

/**
 * Resposta de {@code POST /api/v1/professor/alunos/{alunoId}/relatorios-semestrais} (spec 002,
 * FR-016): quantos relatorios de aula alimentaram a consolidacao e o relatorio gerado.
 */
public record GerarRelatorioSemestralPortalResponseDto(
    int quantidadeRelatoriosEncontrados,
    UUID relatorioSemestralId,
    String status,
    String pdfUrl,
    int versao) {}
