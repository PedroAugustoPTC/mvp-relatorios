package com.escolademusica.relatorios.dto;

import java.time.LocalDate;

/**
 * Item do menu pre-definido de semestres selecionaveis (FR-011, {@code GET
 * /internal/v1/alunos/{alunoId}/semestres-disponiveis}).
 */
public record SemestreDisponivelDto(String chave, String rotulo, LocalDate inicio, LocalDate fim) {}
