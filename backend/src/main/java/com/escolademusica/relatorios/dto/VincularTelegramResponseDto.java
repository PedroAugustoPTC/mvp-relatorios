package com.escolademusica.relatorios.dto;

import java.util.UUID;

/** Resposta de POST /internal/v1/telegram/vinculacao e GET .../telegram/{id}/professor. */
public record VincularTelegramResponseDto(UUID professorId, String nomeProfessor) {}
