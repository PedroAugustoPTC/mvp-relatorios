package com.escolademusica.relatorios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.UUID;

/** Pessoa (possivelmente menor de idade) que recebe aulas. */
@Entity
@Table(name = "aluno")
public class Aluno {

  private static final int IDADE_MAIORIDADE = 18;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "nome", nullable = false)
  private String nome;

  @Column(name = "data_nascimento", nullable = false)
  private LocalDate dataNascimento;

  @Column(name = "cpf_criptografado", nullable = false)
  private byte[] cpfCriptografado;

  @Column(name = "cpf_hash", nullable = false, unique = true)
  private String cpfHash;

  @Column(name = "nome_responsavel")
  private String nomeResponsavel;

  @Column(name = "ativo", nullable = false)
  private boolean ativo = true;

  @Column(name = "criado_em", nullable = false)
  private OffsetDateTime criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;

  public Aluno() {}

  /**
   * Determina se o aluno e menor de idade com base na data de nascimento, considerando a data de
   * referencia informada (tipicamente a data de cadastro).
   */
  public boolean isMenorDeIdadeEm(LocalDate dataReferencia) {
    if (dataNascimento == null || dataReferencia == null) {
      return false;
    }
    return Period.between(dataNascimento, dataReferencia).getYears() < IDADE_MAIORIDADE;
  }

  public boolean isMenorDeIdade() {
    return isMenorDeIdadeEm(LocalDate.now());
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

  public LocalDate getDataNascimento() {
    return dataNascimento;
  }

  public void setDataNascimento(LocalDate dataNascimento) {
    this.dataNascimento = dataNascimento;
  }

  public byte[] getCpfCriptografado() {
    return cpfCriptografado;
  }

  public void setCpfCriptografado(byte[] cpfCriptografado) {
    this.cpfCriptografado = cpfCriptografado;
  }

  public String getCpfHash() {
    return cpfHash;
  }

  public void setCpfHash(String cpfHash) {
    this.cpfHash = cpfHash;
  }

  public String getNomeResponsavel() {
    return nomeResponsavel;
  }

  public void setNomeResponsavel(String nomeResponsavel) {
    this.nomeResponsavel = nomeResponsavel;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public void setAtivo(boolean ativo) {
    this.ativo = ativo;
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
