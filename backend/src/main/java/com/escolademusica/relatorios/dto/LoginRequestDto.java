package com.escolademusica.relatorios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corpo de POST /api/v1/auth/login. */
public record LoginRequestDto(@NotBlank @Email String email, @NotBlank String senha) {}
