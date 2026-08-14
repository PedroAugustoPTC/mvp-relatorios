package com.escolademusica.relatorios.domain.exception;

/** Lancada quando as credenciais informadas pelo administrador (e-mail/senha) sao invalidas. */
public class CredenciaisInvalidasException extends RuntimeException {

  public CredenciaisInvalidasException(String mensagem) {
    super(mensagem);
  }
}
