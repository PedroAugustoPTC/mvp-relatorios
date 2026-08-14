package com.escolademusica.relatorios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de {@code POST /api/v1/professor/auth/vincular} (spec 002, FR-003).
 *
 * @param codigoVinculacao o mesmo codigo de uso unico que o professor ja usa no Telegram
 */
public record VincularCodigoRequestDto(
    @NotBlank(message = "informe o codigo de vinculacao")
        @Size(max = 64, message = "codigo de vinculacao invalido")
        String codigoVinculacao) {}
