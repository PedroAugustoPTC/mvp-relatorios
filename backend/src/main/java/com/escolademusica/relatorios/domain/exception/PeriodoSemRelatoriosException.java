package com.escolademusica.relatorios.domain.exception;

/**
 * Lancada quando se tenta gerar um relatorio semestral para um periodo sem nenhum relatorio de aula
 * aprovado (FR-014). Mapeada pelo GlobalExceptionHandler para HTTP 422 com o codigo estavel {@code
 * PERIODO_SEM_RELATORIOS} (contracts/api-n8n-integration.md).
 */
public class PeriodoSemRelatoriosException extends RuntimeException {

  public PeriodoSemRelatoriosException(String mensagem) {
    super(mensagem);
  }
}
