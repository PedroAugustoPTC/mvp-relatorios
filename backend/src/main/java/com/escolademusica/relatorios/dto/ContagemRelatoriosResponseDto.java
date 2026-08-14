package com.escolademusica.relatorios.dto;

/** Resposta de {@code POST /internal/v1/relatorios-semestrais/contagem} (FR-012, FR-014). */
public record ContagemRelatoriosResponseDto(int quantidadeRelatoriosAula) {}
