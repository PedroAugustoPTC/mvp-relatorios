package com.escolademusica.relatorios.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de {@code POST /api/v1/professor/alunos/{alunoId}/relatorios-semestrais} (spec 002).
 *
 * @param semestre chave do semestre escolhido no menu, no formato {@code "AAAA-S"} (ex.: 2026-1)
 */
public record GerarRelatorioSemestralPortalRequestDto(
    @NotBlank(message = "informe o semestre") String semestre) {}
