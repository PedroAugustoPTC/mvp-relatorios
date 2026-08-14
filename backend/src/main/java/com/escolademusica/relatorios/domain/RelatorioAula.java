package com.escolademusica.relatorios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Registro estruturado gerado a partir do audio de uma aula. */
@Entity
@Table(name = "relatorio_aula")
public class RelatorioAula {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "aula_id", nullable = false, unique = true)
  private UUID aulaId;

  @Column(name = "professor_id", nullable = false)
  private UUID professorId;

  @Column(name = "aluno_id", nullable = false)
  private UUID alunoId;

  @Column(name = "transcricao", columnDefinition = "text")
  private String transcricao;

  @Column(name = "conteudos_trabalhados", columnDefinition = "jsonb")
  private String conteudosTrabalhados;

  @Column(name = "evolucao", columnDefinition = "text")
  private String evolucao;

  @Column(name = "dificuldades", columnDefinition = "jsonb")
  private String dificuldades;

  @Column(name = "atividades_propostas", columnDefinition = "jsonb")
  private String atividadesPropostas;

  @Column(name = "observacoes", columnDefinition = "text")
  private String observacoes;

  @Column(name = "versao", nullable = false)
  private int versao = 1;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private StatusRelatorioAula status = StatusRelatorioAula.RASCUNHO;

  @Column(name = "pdf_url")
  private String pdfUrl;

  @Column(name = "criado_em", nullable = false)
  private OffsetDateTime criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;

  @Column(name = "aprovado_em")
  private OffsetDateTime aprovadoEm;

  public RelatorioAula() {}

  /** RASCUNHO -> PENDENTE_REVISAO quando o PDF e gerado e disponibilizado. */
  public void marcarPendenteRevisao() {
    this.status = StatusRelatorioAula.PENDENTE_REVISAO;
  }

  /**
   * PENDENTE_REVISAO -> PENDENTE_REVISAO (nova versao) quando o professor solicita alteracao;
   * incrementa versao (FR-009).
   */
  public void solicitarAlteracao() {
    this.versao += 1;
  }

  /**
   * PENDENTE_REVISAO -> APROVADO mediante confirmacao explicita do professor sobre o PDF vigente
   * (FR-008a).
   */
  public void aprovar(OffsetDateTime momento) {
    if (this.status != StatusRelatorioAula.PENDENTE_REVISAO) {
      throw new IllegalStateException(
          "Relatorio de aula so pode ser aprovado a partir do status PENDENTE_REVISAO");
    }
    this.status = StatusRelatorioAula.APROVADO;
    this.aprovadoEm = momento;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getAulaId() {
    return aulaId;
  }

  public void setAulaId(UUID aulaId) {
    this.aulaId = aulaId;
  }

  public UUID getProfessorId() {
    return professorId;
  }

  public void setProfessorId(UUID professorId) {
    this.professorId = professorId;
  }

  public UUID getAlunoId() {
    return alunoId;
  }

  public void setAlunoId(UUID alunoId) {
    this.alunoId = alunoId;
  }

  public String getTranscricao() {
    return transcricao;
  }

  public void setTranscricao(String transcricao) {
    this.transcricao = transcricao;
  }

  public String getConteudosTrabalhados() {
    return conteudosTrabalhados;
  }

  public void setConteudosTrabalhados(String conteudosTrabalhados) {
    this.conteudosTrabalhados = conteudosTrabalhados;
  }

  public String getEvolucao() {
    return evolucao;
  }

  public void setEvolucao(String evolucao) {
    this.evolucao = evolucao;
  }

  public String getDificuldades() {
    return dificuldades;
  }

  public void setDificuldades(String dificuldades) {
    this.dificuldades = dificuldades;
  }

  public String getAtividadesPropostas() {
    return atividadesPropostas;
  }

  public void setAtividadesPropostas(String atividadesPropostas) {
    this.atividadesPropostas = atividadesPropostas;
  }

  public String getObservacoes() {
    return observacoes;
  }

  public void setObservacoes(String observacoes) {
    this.observacoes = observacoes;
  }

  public int getVersao() {
    return versao;
  }

  public void setVersao(int versao) {
    this.versao = versao;
  }

  public StatusRelatorioAula getStatus() {
    return status;
  }

  public void setStatus(StatusRelatorioAula status) {
    this.status = status;
  }

  public String getPdfUrl() {
    return pdfUrl;
  }

  public void setPdfUrl(String pdfUrl) {
    this.pdfUrl = pdfUrl;
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

  public OffsetDateTime getAprovadoEm() {
    return aprovadoEm;
  }

  public void setAprovadoEm(OffsetDateTime aprovadoEm) {
    this.aprovadoEm = aprovadoEm;
  }

  /** Status possiveis de um RelatorioAula (FR-021). */
  public enum StatusRelatorioAula {
    RASCUNHO,
    PENDENTE_REVISAO,
    APROVADO
  }
}
