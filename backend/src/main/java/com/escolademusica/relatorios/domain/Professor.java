package com.escolademusica.relatorios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Pessoa que ministra aulas e usa o Telegram para registrar relatorios. */
@Entity
@Table(name = "professor")
public class Professor {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "nome", nullable = false)
  private String nome;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  @Column(name = "codigo_vinculacao", unique = true)
  private String codigoVinculacao;

  @Column(name = "codigo_vinculacao_expira_em")
  private OffsetDateTime codigoVinculacaoExpiraEm;

  @Column(name = "criado_em", nullable = false)
  private OffsetDateTime criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;

  public Professor() {}

  public boolean codigoVinculacaoValidoEm(OffsetDateTime momento) {
    return codigoVinculacao != null
        && codigoVinculacaoExpiraEm != null
        && momento.isBefore(codigoVinculacaoExpiraEm);
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
  }

  public String getCodigoVinculacao() {
    return codigoVinculacao;
  }

  public void setCodigoVinculacao(String codigoVinculacao) {
    this.codigoVinculacao = codigoVinculacao;
  }

  public OffsetDateTime getCodigoVinculacaoExpiraEm() {
    return codigoVinculacaoExpiraEm;
  }

  public void setCodigoVinculacaoExpiraEm(OffsetDateTime codigoVinculacaoExpiraEm) {
    this.codigoVinculacaoExpiraEm = codigoVinculacaoExpiraEm;
  }

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public void setCriadoEm(OffsetDateTime criadoEm) {
    this.criadoEm = criadoEm;
  }

  public OffsetDateTime getAtualizadoEm() {
    return atualizadoEm;
  }

  public void setAtualizadoEm(OffsetDateTime atualizadoEm) {
    this.atualizadoEm = atualizadoEm;
  }
}
