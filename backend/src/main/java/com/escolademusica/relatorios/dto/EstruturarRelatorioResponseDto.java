package com.escolademusica.relatorios.dto;

import java.util.List;
import java.util.UUID;

/**
 * Resposta comum aos endpoints {@code .../estruturar}, {@code .../responder-pergunta} e {@code
 * .../revisar} de relatorio de aula (T047, contracts/api-n8n-integration.md).
 */
public record EstruturarRelatorioResponseDto(
    UUID relatorioId,
    String status,
    List<String> conteudosTrabalhados,
    String evolucao,
    List<String> dificuldades,
    List<String> atividadesPropostas,
    String observacoes,
    List<String> perguntasPendentes,
    String pdfUrl,
    int versao) {}
