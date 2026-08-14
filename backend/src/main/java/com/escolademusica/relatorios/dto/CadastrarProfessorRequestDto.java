package com.escolademusica.relatorios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corpo de POST /api/v1/professores. */
public record CadastrarProfessorRequestDto(@NotBlank String nome, @NotBlank @Email String email) {}
