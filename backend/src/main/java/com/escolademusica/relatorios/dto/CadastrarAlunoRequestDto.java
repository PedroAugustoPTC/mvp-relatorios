package com.escolademusica.relatorios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Corpo de POST /api/v1/alunos. {@code nomeResponsavel} e obrigatorio apenas quando menor. */
public record CadastrarAlunoRequestDto(
    @NotBlank String nome,
    @NotNull LocalDate dataNascimento,
    @NotBlank String cpf,
    String nomeResponsavel,
    List<UUID> professorIds) {}
