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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "conteudos_trabalhados", columnDefinition = "jsonb")
  private String conteudosTrabalhados;

  @Column(name = "evolucao", columnDefinition = "text")
  private String evolucao;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "dificuldades", columnDefinition = "jsonb")
  private String dificuldades;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "atividades_propostas", columnDefinition = "jsonb")
  private String atividadesPropostas;

  @Column(name = "observacoes", columnDefinition = "text")
  private String observacoes;

  /**
   * Perguntas de acompanhamento que o professor ainda precisa responder (FR-007), como JSON.
   *
   * <p>Persistidas para que o acompanhamento por polling ({@code GET /relatorios-aula/{id}})
   * consiga devolve-las: sem isso um relatorio em RASCUNHO aguardando resposta ficava
   * indistinguivel de um relatorio ainda em processamento.
   *
   * <p>O default {@code []} vale para o relatorio recem-criado, que nasce antes da estruturacao e
   * portanto ainda nao tem pendencia alguma. Ele precisa estar aqui, e nao so no DEFAULT da coluna:
   * o Hibernate sempre inclui a coluna no INSERT, entao um campo nulo viraria um NULL explicito e o
   * DEFAULT do banco nunca seria aplicado.
   */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "perguntas_pendentes", nullable = false, columnDefinition = "jsonb")
  private String perguntasPendentes = "[]";

  @Column(name = "versao", nullable = false)
  private int versao = 1;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private StatusRelatorioAula status = StatusRelatorioAula.RASCUNHO;

  @Column(name = "pdf_url")
  private String pdfUrl;

  /**
   * Canal que originou o relatorio (spec 002, FR-002). O default {@code TELEGRAM} espelha o default
   * da coluna na migracao V9 e preserva o comportamento do fluxo ja existente do bot; o portal web
   * sobrescreve explicitamente com {@code WEB} no seu adaptador de entrada.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "canal_origem", nullable = false, length = 16)
  private CanalOrigem canalOrigem = CanalOrigem.TELEGRAM;

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

  /**
   * Encerra definitivamente um relatorio que o professor decidiu nao concluir (spec 002). Um
   * relatorio ja aprovado e um documento oficial entregue e nunca pode ser cancelado.
   */
  public void cancelar() {
    if (this.status == StatusRelatorioAula.APROVADO) {
      throw new IllegalStateException("Relatorio de aula ja aprovado nao pode ser cancelado");
    }
    if (this.status == StatusRelatorioAula.CANCELADO) {
      throw new IllegalStateException("Relatorio de aula ja esta cancelado");
    }
    this.status = StatusRelatorioAula.CANCELADO;
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

  public String getPerguntasPendentes() {
    return perguntasPendentes;
  }

  public void setPerguntasPendentes(String perguntasPendentes) {
    this.perguntasPendentes = perguntasPendentes;
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

  public CanalOrigem getCanalOrigem() {
    return canalOrigem;
  }

  public void setCanalOrigem(CanalOrigem canalOrigem) {
    this.canalOrigem = canalOrigem;
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

  /** Status possiveis de um RelatorioAula (FR-021, + CANCELADO na spec 002). */
  public enum StatusRelatorioAula {
    RASCUNHO,
    PENDENTE_REVISAO,
    APROVADO,
    CANCELADO
  }
}
