package com.escolademusica.relatorios.dto;

import jakarta.validation.constraints.NotBlank;

/** Corpo de {@code POST /internal/v1/telegram/{telegramUserId}/alunos/selecionar}. */
public record SelecionarAlunoRequestDto(@NotBlank String nomeAluno) {}
