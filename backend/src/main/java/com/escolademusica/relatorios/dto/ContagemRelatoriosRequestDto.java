package com.escolademusica.relatorios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Corpo de {@code POST /internal/v1/relatorios-semestrais/contagem} (FR-012). */
public record ContagemRelatoriosRequestDto(@NotNull UUID alunoId, @NotBlank String periodoChave) {}
