package com.escolademusica.relatorios.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Resposta 201 de POST /api/v1/professores e 200 de POST .../codigo-vinculacao. */
public record ProfessorResponseDto(
    UUID id,
    String nome,
    String email,
    String codigoVinculacao,
    OffsetDateTime codigoVinculacaoExpiraEm) {}
