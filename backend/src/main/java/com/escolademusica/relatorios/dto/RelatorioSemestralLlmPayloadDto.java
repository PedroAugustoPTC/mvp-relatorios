package com.escolademusica.relatorios.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

/**
 * Formato JSON trocado com o {@code LlmGateway} para consolidar/revisar um relatorio semestral
 * (data-model.md, campos de {@code RelatorioSemestral}; FR-013). Cada secao e mantida como {@link
 * JsonNode} generico porque o modelo de referencia da escola define sub-campos ricos por secao
 * (ex.: "informacoesGerais" contem nome, idade, nivel, professor, periodo) que nao precisam ser
 * conhecidos rigidamente pelo backend — apenas validados como JSON e persistidos/exibidos no PDF.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RelatorioSemestralLlmPayloadDto(
    JsonNode informacoesGerais,
    JsonNode frequenciaEEstudo,
    JsonNode tecnica,
    JsonNode musicalidade,
    JsonNode leituraEMemorizacao,
    JsonNode pontosDeAtencao,
    JsonNode estrategiasPedagogicas,
    JsonNode acompanhamentoFamiliar,
    JsonNode planejamentoProximoSemestre,
    String parecerFinal) {

  public RelatorioSemestralLlmPayloadDto {
    informacoesGerais = informacoesGerais == null ? NullNode.getInstance() : informacoesGerais;
    frequenciaEEstudo = frequenciaEEstudo == null ? NullNode.getInstance() : frequenciaEEstudo;
    tecnica = tecnica == null ? NullNode.getInstance() : tecnica;
    musicalidade = musicalidade == null ? NullNode.getInstance() : musicalidade;
    leituraEMemorizacao =
        leituraEMemorizacao == null ? NullNode.getInstance() : leituraEMemorizacao;
    pontosDeAtencao = pontosDeAtencao == null ? NullNode.getInstance() : pontosDeAtencao;
    estrategiasPedagogicas =
        estrategiasPedagogicas == null ? NullNode.getInstance() : estrategiasPedagogicas;
    acompanhamentoFamiliar =
        acompanhamentoFamiliar == null ? NullNode.getInstance() : acompanhamentoFamiliar;
    planejamentoProximoSemestre =
        planejamentoProximoSemestre == null ? NullNode.getInstance() : planejamentoProximoSemestre;
    parecerFinal = parecerFinal == null ? "" : parecerFinal;
  }
}
