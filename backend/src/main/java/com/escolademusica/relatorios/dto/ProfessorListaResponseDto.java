package com.escolademusica.relatorios.dto;

import java.util.UUID;

/** Item de resposta de GET /api/v1/professores. */
public record ProfessorListaResponseDto(
    UUID id, String nome, String email, boolean vinculadoTelegram) {}
