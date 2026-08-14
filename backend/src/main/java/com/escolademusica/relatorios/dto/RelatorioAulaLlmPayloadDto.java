package com.escolademusica.relatorios.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Formato JSON trocado com o {@code LlmGateway} para estruturar/revisar um relatorio de aula
 * (data-model.md, campos de {@code RelatorioAula}). Usado tanto para desserializar a resposta do
 * LLM quanto para serializar o estado atual do relatorio ao pedir uma revisao (T044).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelatorioAulaLlmPayloadDto(
    List<String> conteudosTrabalhados,
    String evolucao,
    List<String> dificuldades,
    List<String> atividadesPropostas,
    String observacoes,
    List<String> perguntasPendentes) {

  public RelatorioAulaLlmPayloadDto {
    conteudosTrabalhados = conteudosTrabalhados == null ? List.of() : conteudosTrabalhados;
    evolucao = evolucao == null ? "" : evolucao;
    dificuldades = dificuldades == null ? List.of() : dificuldades;
    atividadesPropostas = atividadesPropostas == null ? List.of() : atividadesPropostas;
    observacoes = observacoes == null ? "" : observacoes;
    perguntasPendentes = perguntasPendentes == null ? List.of() : perguntasPendentes;
  }
}
