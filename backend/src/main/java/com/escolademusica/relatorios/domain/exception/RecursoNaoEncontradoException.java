package com.escolademusica.relatorios.domain.exception;

/** Lancada quando um recurso de dominio solicitado (por id ou chave de busca) nao existe. */
public class RecursoNaoEncontradoException extends RuntimeException {

  public RecursoNaoEncontradoException(String mensagem) {
    super(mensagem);
  }
}
