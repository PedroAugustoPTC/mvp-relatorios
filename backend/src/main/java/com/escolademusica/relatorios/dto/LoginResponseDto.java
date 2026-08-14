package com.escolademusica.relatorios.dto;

import java.time.OffsetDateTime;

/** Resposta 200 de POST /api/v1/auth/login. */
public record LoginResponseDto(String token, OffsetDateTime expiraEm) {}
