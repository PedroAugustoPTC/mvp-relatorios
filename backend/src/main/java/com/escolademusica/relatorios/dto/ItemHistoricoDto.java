package com.escolademusica.relatorios.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Item unificado do historico de um aluno (US4, FR-017): relatorio de aula ou semestral. Campos nao
 * aplicaveis ao {@code tipo} do item permanecem {@code null} (ex.: {@code periodoInicio} em um item
 * {@code AULA}).
 */
public record ItemHistoricoDto(
    String tipo,
    UUID id,
    LocalDate dataAula,
    LocalDate periodoInicio,
    LocalDate periodoFim,
    String status,
    String pdfUrl,
    OffsetDateTime aprovadoEm) {

  public static ItemHistoricoDto deAula(
      UUID id, LocalDate dataAula, String status, String pdfUrl, OffsetDateTime aprovadoEm) {
    return new ItemHistoricoDto("AULA", id, dataAula, null, null, status, pdfUrl, aprovadoEm);
  }

  public static ItemHistoricoDto deSemestral(
      UUID id,
      LocalDate periodoInicio,
      LocalDate periodoFim,
      String status,
      String pdfUrl,
      OffsetDateTime aprovadoEm) {
    return new ItemHistoricoDto(
        "SEMESTRAL", id, null, periodoInicio, periodoFim, status, pdfUrl, aprovadoEm);
  }

  /** Data usada para ordenar cronologicamente o historico combinado. */
  public LocalDate dataOrdenacao() {
    return dataAula != null ? dataAula : periodoInicio;
  }
}
