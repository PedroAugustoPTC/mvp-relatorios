package com.escolademusica.relatorios.domain;

/**
 * Canal pelo qual um relatorio foi criado (spec 002, FR-002). Existe apenas como dado registrado em
 * {@link RelatorioAula}/{@link RelatorioSemestral} — nenhuma regra de negocio se ramifica por
 * canal; os UseCases permanecem agnosticos e apenas os adaptadores de entrada (Controllers de cada
 * canal) informam o valor.
 */
public enum CanalOrigem {
  TELEGRAM,
  WEB
}
