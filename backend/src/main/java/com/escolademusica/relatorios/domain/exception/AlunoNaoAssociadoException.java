package com.escolademusica.relatorios.domain.exception;

/**
 * Lancada quando um professor tenta acessar/registrar dados de um aluno ao qual nao esta associado
 * via {@code ProfessorAluno} (FR-018).
 */
public class AlunoNaoAssociadoException extends RuntimeException {

  public AlunoNaoAssociadoException(String mensagem) {
    super(mensagem);
  }
}
