package com.escolademusica.relatorios.dto;

import java.util.UUID;

/** Item da lista de selecao de aluno do bot (T048, FR-004): {@code GET .../alunos}. */
public record AlunoResumoDto(UUID id, String nome) {}
