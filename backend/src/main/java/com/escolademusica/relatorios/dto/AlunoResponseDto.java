package com.escolademusica.relatorios.dto;

import java.time.LocalDate;
import java.util.UUID;

/** Resposta de POST/GET /api/v1/alunos. Nunca expoe o CPF em claro (contracts/api-web-admin.md). */
public record AlunoResponseDto(
    UUID id, String nome, LocalDate dataNascimento, String nomeResponsavel, boolean menorDeIdade) {}
