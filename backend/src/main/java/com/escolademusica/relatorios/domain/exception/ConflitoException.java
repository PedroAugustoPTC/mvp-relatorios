package com.escolademusica.relatorios.domain.exception;

/**
 * Lancada quando uma operacao viola uma regra de unicidade/estado do dominio (ex.: e-mail de
 * professor duplicado, CPF de aluno duplicado, conta do Telegram ja vinculada a outro professor).
 * Carrega um {@code codigo} estavel para que os consumidores da API (incluindo o n8n) possam
 * distinguir o motivo do conflito sem acoplar texto de negocio (ver contracts/*.md, secao "Erros").
 */
public class ConflitoException extends RuntimeException {

  private final String codigo;

  public ConflitoException(String codigo, String mensagem) {
    super(mensagem);
    this.codigo = codigo;
  }

  public String getCodigo() {
    return codigo;
  }
}
