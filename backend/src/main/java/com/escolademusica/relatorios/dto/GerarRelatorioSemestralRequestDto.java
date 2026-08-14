package com.escolademusica.relatorios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Corpo de {@code POST /internal/v1/relatorios-semestrais} (FR-013, FR-014). */
public record GerarRelatorioSemestralRequestDto(
    @NotNull UUID alunoId, @NotNull UUID professorId, @NotBlank String periodoChave) {}
