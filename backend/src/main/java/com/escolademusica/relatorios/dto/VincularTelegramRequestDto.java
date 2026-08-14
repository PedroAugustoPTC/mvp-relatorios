package com.escolademusica.relatorios.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo de POST /internal/v1/telegram/vinculacao. */
public record VincularTelegramRequestDto(
    @NotBlank String telegramUserId, @NotBlank String codigoVinculacao) {}
