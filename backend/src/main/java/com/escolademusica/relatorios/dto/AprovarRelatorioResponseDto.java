package com.escolademusica.relatorios.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Resposta de {@code POST /internal/v1/relatorios-aula/{relatorioId}/aprovar}. */
public record AprovarRelatorioResponseDto(
    UUID relatorioId, String status, OffsetDateTime aprovadoEm) {}
