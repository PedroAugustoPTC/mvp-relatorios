package com.escolademusica.relatorios.dto;

import java.util.UUID;

/** Resposta de {@code POST /internal/v1/relatorios-aula/transcrever} (T047). */
public record TranscreverResponseDto(UUID aulaId, String transcricao) {}
