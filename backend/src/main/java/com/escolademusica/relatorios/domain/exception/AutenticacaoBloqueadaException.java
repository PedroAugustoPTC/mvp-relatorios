package com.escolademusica.relatorios.domain.exception;

/**
 * Tentativas de autenticacao no portal web temporariamente bloqueadas apos exceder o limite de
 * codigos incorretos consecutivos (spec 002, FR-004a).
 *
 * @see com.escolademusica.relatorios.service.RateLimiterAutenticacaoWebService
 */
public class AutenticacaoBloqueadaException extends RuntimeException {

  private final long retryAfterSeconds;

  public AutenticacaoBloqueadaException(String mensagem, long retryAfterSeconds) {
    super(mensagem);
    this.retryAfterSeconds = retryAfterSeconds;
  }

  /** Quanto o professor precisa esperar antes de tentar de novo. */
  public long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}
