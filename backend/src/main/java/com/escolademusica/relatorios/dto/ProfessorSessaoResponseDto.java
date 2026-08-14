package com.escolademusica.relatorios.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Resposta de {@code GET /api/v1/professor/me} (spec 002, contracts/portal-web-api.md): dados do
 * professor autenticado e o instante de expiracao da sessao vigente, para o frontend decidir quando
 * avisar/pedir reautenticacao.
 */
public record ProfessorSessaoResponseDto(UUID id, String nome, OffsetDateTime sessaoExpiraEm) {}
