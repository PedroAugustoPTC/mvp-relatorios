package com.escolademusica.relatorios.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Corpo de {@code POST /internal/v1/relatorios-aula/{relatorioId}/aprovar} (T045, FR-008a/FR-010).
 * O professor deve confirmar explicitamente a versao do PDF que esta aprovando; se ela nao bater
 * com a versao vigente do relatorio (ex.: uma revisao foi gerada nesse meio tempo), a aprovacao e
 * rejeitada com 409 para evitar aprovar um PDF desatualizado.
 */
public record AprovarRelatorioRequestDto(@NotNull Integer versaoConfirmada) {}
