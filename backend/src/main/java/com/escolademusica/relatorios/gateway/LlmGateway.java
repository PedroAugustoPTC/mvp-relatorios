package com.escolademusica.relatorios.gateway;

import java.util.List;

/**
 * Porta para o provedor de LLM usado para estruturar/consolidar relatorios (research.md secao 2).
 * Ambos os metodos seguem o mesmo padrao "texto de entrada -> JSON estruturado segundo um schema",
 * validado pelo backend antes de qualquer persistencia (ver contracts/). Implementacoes concretas
 * chegam nas fases T039 (relatorio de aula) e T077 (relatorio semestral).
 */
public interface LlmGateway {

  /**
   * Estrutura a transcricao de uma aula em um relatorio de aula.
   *
   * @param transcricao texto transcrito do audio da aula (ver {@link SpeechToTextGateway})
   * @return JSON estruturado com os campos do relatorio de aula (conteudosTrabalhados, evolucao,
   *     dificuldades, atividadesPropostas, observacoes — ver data-model.md), a ser validado pelo
   *     backend antes de persistir
   */
  String estruturarRelatorioAula(String transcricao);

  /**
   * Aplica uma instrucao de alteracao em linguagem natural sobre um relatorio de aula ja
   * estruturado (FR-009).
   *
   * @param relatorioAtualJson JSON estruturado atual do relatorio de aula
   * @param instrucao instrucao em linguagem natural descrevendo a alteracao desejada
   * @return novo JSON estruturado com a alteracao aplicada, a ser validado pelo backend antes de
   *     persistir
   */
  String revisarRelatorioAula(String relatorioAtualJson, String instrucao);

  /**
   * Consolida multiplos relatorios de aula de um periodo em um relatorio semestral.
   *
   * @param relatoriosAulaJson JSON de cada relatorio de aula considerado no periodo
   * @return JSON estruturado com as secoes do relatorio semestral (ver data-model.md), a ser
   *     validado pelo backend antes de persistir
   */
  String consolidarRelatorioSemestral(List<String> relatoriosAulaJson);

  /**
   * Aplica uma instrucao de alteracao em linguagem natural sobre um relatorio semestral ja
   * consolidado (FR-015).
   *
   * @param relatorioAtualJson JSON estruturado atual do relatorio semestral
   * @param instrucao instrucao em linguagem natural descrevendo a alteracao desejada
   * @return novo JSON estruturado com a alteracao aplicada, a ser validado pelo backend antes de
   *     persistir
   */
  String revisarRelatorioSemestral(String relatorioAtualJson, String instrucao);
}
