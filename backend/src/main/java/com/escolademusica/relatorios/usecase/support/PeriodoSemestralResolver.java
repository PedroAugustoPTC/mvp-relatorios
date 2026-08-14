package com.escolademusica.relatorios.usecase.support;

import java.time.LocalDate;

/**
 * Resolve a chave de um semestre pre-definido (ex.: {@code "2026-1"}) para o rotulo e o intervalo
 * de datas correspondente (FR-011). Compartilhado por {@code ListarSemestresDisponiveisUseCase},
 * {@code ContarRelatoriosPeriodoUseCase} e {@code GerarRelatorioSemestralUseCase} para garantir que
 * todos calculem exatamente o mesmo periodo a partir da mesma chave.
 */
public final class PeriodoSemestralResolver {

  private PeriodoSemestralResolver() {}

  /** Um semestre selecionavel: chave estavel, rotulo amigavel e intervalo de datas. */
  public record Periodo(String chave, String rotulo, LocalDate inicio, LocalDate fim) {}

  public static Periodo resolver(int ano, int semestre) {
    if (semestre == 1) {
      return new Periodo(
          ano + "-1", "1º semestre " + ano, LocalDate.of(ano, 1, 1), LocalDate.of(ano, 6, 30));
    }
    if (semestre == 2) {
      return new Periodo(
          ano + "-2", "2º semestre " + ano, LocalDate.of(ano, 7, 1), LocalDate.of(ano, 12, 31));
    }
    throw new IllegalArgumentException("Semestre invalido: " + semestre + " (deve ser 1 ou 2)");
  }

  /**
   * Resolve o periodo a partir de uma chave no formato {@code "AAAA-S"} (ex.: {@code "2026-1"}).
   */
  public static Periodo resolverPorChave(String periodoChave) {
    if (periodoChave == null || periodoChave.isBlank()) {
      throw new IllegalArgumentException("periodoChave e obrigatoria");
    }
    String[] partes = periodoChave.split("-");
    if (partes.length != 2) {
      throw new IllegalArgumentException("periodoChave invalida: " + periodoChave);
    }
    try {
      int ano = Integer.parseInt(partes[0]);
      int semestre = Integer.parseInt(partes[1]);
      return resolver(ano, semestre);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("periodoChave invalida: " + periodoChave, e);
    }
  }
}
