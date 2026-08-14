package com.escolademusica.relatorios.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo de {@code POST /internal/v1/relatorios-aula/{relatorioId}/revisar}. */
public record RevisarRelatorioRequestDto(@NotBlank String instrucao) {}
