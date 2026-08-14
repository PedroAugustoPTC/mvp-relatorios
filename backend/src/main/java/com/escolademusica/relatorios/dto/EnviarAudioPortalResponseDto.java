package com.escolademusica.relatorios.dto;

import java.util.UUID;

/**
 * Resposta de {@code POST /api/v1/professor/alunos/{alunoId}/relatorios-aula/audio} (spec 002).
 *
 * @param relatorioId id do relatorio criado, usado pelo frontend para acompanhar o processamento
 * @param status situacao atual do relatorio ({@code PENDENTE_REVISAO} quando o texto ja pode ser
 *     conferido, ou {@code RASCUNHO} quando ainda ha perguntas de acompanhamento a responder)
 * @param perguntasPendentes perguntas que o professor precisa responder antes de o PDF ser gerado
 *     (FR-010); vazio quando o relatorio ja esta completo
 */
public record EnviarAudioPortalResponseDto(
    UUID relatorioId, String status, java.util.List<String> perguntasPendentes) {}
