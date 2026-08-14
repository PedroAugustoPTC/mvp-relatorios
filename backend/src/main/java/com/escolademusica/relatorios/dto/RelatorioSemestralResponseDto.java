package com.escolademusica.relatorios.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

/**
 * Resposta comum aos endpoints de geracao/revisao de relatorio semestral (T079,
 * contracts/api-n8n-integration.md, secao "Relatorio semestral (US3)").
 */
public record RelatorioSemestralResponseDto(
    UUID relatorioId,
    String status,
    int quantidadeRelatoriosAulaConsiderados,
    JsonNode informacoesGerais,
    JsonNode frequenciaEEstudo,
    JsonNode tecnica,
    JsonNode musicalidade,
    JsonNode leituraEMemorizacao,
    JsonNode pontosDeAtencao,
    JsonNode estrategiasPedagogicas,
    JsonNode acompanhamentoFamiliar,
    JsonNode planejamentoProximoSemestre,
    String parecerFinal,
    String pdfUrl,
    int versao) {}
