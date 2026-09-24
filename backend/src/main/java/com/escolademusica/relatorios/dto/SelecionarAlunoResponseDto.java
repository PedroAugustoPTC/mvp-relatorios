package com.escolademusica.relatorios.dto;

import java.util.UUID;

/**
 * Resposta de {@code POST /internal/v1/telegram/{telegramUserId}/alunos/selecionar}.
 *
 * <p>Quando {@code encontrado} e falso, o n8n deve reexibir o menu de alunos (nenhum estado e
 * alterado no backend) — o professor digitou algo que nao bateu com nenhum nome da lista.
 */
public record SelecionarAlunoResponseDto(boolean encontrado, UUID alunoId, String nomeAluno) {}
