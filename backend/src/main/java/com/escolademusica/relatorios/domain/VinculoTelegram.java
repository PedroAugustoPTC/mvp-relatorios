package com.escolademusica.relatorios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Associacao entre o identificador de usuario do Telegram e o cadastro de um professor. Apenas
 * identificador de integracao; nunca e tratado como prova de autenticacao para acoes
 * administrativas sensiveis (FR-020).
 */
@Entity
@Table(name = "vinculo_telegram")
public class VinculoTelegram {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "professor_id", nullable = false, unique = true)
  private UUID professorId;

  @Column(name = "telegram_user_id", nullable = false, unique = true)
  private String telegramUserId;

  @Column(name = "vinculado_em", nullable = false)
  private OffsetDateTime vinculadoEm;

  /**
   * Aluno que o professor escolheu na mensagem de texto mais recente, antes de enviar o audio da
   * aula (bot do Telegram, canal exclusivo de registro de aula). Cada mensagem do Telegram dispara
   * uma execucao nova e isolada do n8n; essa selecao precisa ficar aqui para sobreviver de uma
   * mensagem para a proxima.
   */
  @Column(name = "aluno_selecionado_id")
  private UUID alunoSelecionadoId;

  @Column(name = "aluno_selecionado_em")
  private OffsetDateTime alunoSelecionadoEm;

  public VinculoTelegram() {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getProfessorId() {
    return professorId;
  }

  public void setProfessorId(UUID professorId) {
    this.professorId = professorId;
  }

  public String getTelegramUserId() {
    return telegramUserId;
  }

  public void setTelegramUserId(String telegramUserId) {
    this.telegramUserId = telegramUserId;
  }

  public OffsetDateTime getVinculadoEm() {
    return vinculadoEm;
  }

  public void setVinculadoEm(OffsetDateTime vinculadoEm) {
    this.vinculadoEm = vinculadoEm;
  }

  public UUID getAlunoSelecionadoId() {
    return alunoSelecionadoId;
  }

  public void setAlunoSelecionadoId(UUID alunoSelecionadoId) {
    this.alunoSelecionadoId = alunoSelecionadoId;
  }

  public OffsetDateTime getAlunoSelecionadoEm() {
    return alunoSelecionadoEm;
  }

  public void setAlunoSelecionadoEm(OffsetDateTime alunoSelecionadoEm) {
    this.alunoSelecionadoEm = alunoSelecionadoEm;
  }
}
