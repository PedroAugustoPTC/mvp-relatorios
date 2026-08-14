package com.escolademusica.relatorios.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resposta de {@code POST /api/v1/professor/auth/vincular} (spec 002): o token da sessao do portal
 * e a identificacao do professor autenticado.
 */
public record AutenticacaoProfessorResponseDto(
    String token, OffsetDateTime expiraEm, ProfessorAutenticadoDto professor) {

  /** Dados minimos do professor exibidos pelo portal apos o login. */
  public record ProfessorAutenticadoDto(UUID id, String nome) {}
}
