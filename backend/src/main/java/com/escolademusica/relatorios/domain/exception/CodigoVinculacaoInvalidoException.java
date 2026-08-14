package com.escolademusica.relatorios.domain.exception;

/**
 * Codigo de vinculacao informado no portal web e inexistente ou expirado (spec 002, FR-004).
 *
 * <p>A mensagem exposta ao professor e deliberadamente generica e identica nos dois casos: dizer
 * "codigo existe mas expirou" versus "codigo nao existe" entregaria a um atacante um oraculo para
 * descobrir codigos validos por tentativa e erro.
 */
public class CodigoVinculacaoInvalidoException extends RuntimeException {

  public CodigoVinculacaoInvalidoException(String mensagem) {
    super(mensagem);
  }
}
