package com.escolademusica.relatorios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Consolidacao de multiplos relatorios de aula de um aluno em um periodo (semestre). */
@Entity
@Table(name = "relatorio_semestral")
public class RelatorioSemestral {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "aluno_id", nullable = false)
  private UUID alunoId;

  @Column(name = "professor_id", nullable = false)
  private UUID professorId;

  @Column(name = "periodo_inicio", nullable = false)
  private LocalDate periodoInicio;

  @Column(name = "periodo_fim", nullable = false)
  private LocalDate periodoFim;

  @Column(name = "quantidade_relatorios_aula_considerados", nullable = false)
  private int quantidadeRelatoriosAulaConsiderados;

  @Column(name = "informacoes_gerais", columnDefinition = "jsonb")
  private String informacoesGerais;

  @Column(name = "frequencia_e_estudo", columnDefinition = "jsonb")
  private String frequenciaEEstudo;

  @Column(name = "tecnica", columnDefinition = "jsonb")
  private String tecnica;

  @Column(name = "musicalidade", columnDefinition = "jsonb")
  private String musicalidade;

  @Column(name = "leitura_e_memorizacao", columnDefinition = "jsonb")
  private String leituraEMemorizacao;

  @Column(name = "pontos_de_atencao", columnDefinition = "jsonb")
  private String pontosDeAtencao;

  @Column(name = "estrategias_pedagogicas", columnDefinition = "jsonb")
  private String estrategiasPedagogicas;

  @Column(name = "acompanhamento_familiar", columnDefinition = "jsonb")
  private String acompanhamentoFamiliar;

  @Column(name = "planejamento_proximo_semestre", columnDefinition = "jsonb")
  private String planejamentoProximoSemestre;

  @Column(name = "parecer_final", columnDefinition = "text")
  private String parecerFinal;

  @Column(name = "versao", nullable = false)
  private int versao = 1;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private StatusRelatorioSemestral status = StatusRelatorioSemestral.PENDENTE_REVISAO;

  @Column(name = "pdf_url")
  private String pdfUrl;

  @Column(name = "criado_em", nullable = false)
  private OffsetDateTime criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;

  @Column(name = "aprovado_em")
  private OffsetDateTime aprovadoEm;

  public RelatorioSemestral() {}

  /** Incrementa a versao a cada alteracao solicitada (FR-015). */
  public void solicitarAlteracao() {
    this.versao += 1;
  }

  /** PENDENTE_REVISAO -> APROVADO mediante confirmacao explicita sobre o PDF vigente (FR-015). */
  public void aprovar(OffsetDateTime momento) {
    if (this.status != StatusRelatorioSemestral.PENDENTE_REVISAO) {
      throw new IllegalStateException(
          "Relatorio semestral so pode ser aprovado a partir do status PENDENTE_REVISAO");
    }
    this.status = StatusRelatorioSemestral.APROVADO;
    this.aprovadoEm = momento;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getAlunoId() {
    return alunoId;
  }

  public void setAlunoId(UUID alunoId) {
    this.alunoId = alunoId;
  }

  public UUID getProfessorId() {
    return professorId;
  }

  public void setProfessorId(UUID professorId) {
    this.professorId = professorId;
  }

  public LocalDate getPeriodoInicio() {
    return periodoInicio;
  }

  public void setPeriodoInicio(LocalDate periodoInicio) {
    this.periodoInicio = periodoInicio;
  }

  public LocalDate getPeriodoFim() {
    return periodoFim;
  }

  public void setPeriodoFim(LocalDate periodoFim) {
    this.periodoFim = periodoFim;
  }

  public int getQuantidadeRelatoriosAulaConsiderados() {
    return quantidadeRelatoriosAulaConsiderados;
  }

  public void setQuantidadeRelatoriosAulaConsiderados(int quantidadeRelatoriosAulaConsiderados) {
    this.quantidadeRelatoriosAulaConsiderados = quantidadeRelatoriosAulaConsiderados;
  }

  public String getInformacoesGerais() {
    return informacoesGerais;
  }

  public void setInformacoesGerais(String informacoesGerais) {
    this.informacoesGerais = informacoesGerais;
  }

  public String getFrequenciaEEstudo() {
    return frequenciaEEstudo;
  }

  public void setFrequenciaEEstudo(String frequenciaEEstudo) {
    this.frequenciaEEstudo = frequenciaEEstudo;
  }

  public String getTecnica() {
    return tecnica;
  }

  public void setTecnica(String tecnica) {
    this.tecnica = tecnica;
  }

  public String getMusicalidade() {
    return musicalidade;
  }

  public void setMusicalidade(String musicalidade) {
    this.musicalidade = musicalidade;
  }

  public String getLeituraEMemorizacao() {
    return leituraEMemorizacao;
  }

  public void setLeituraEMemorizacao(String leituraEMemorizacao) {
    this.leituraEMemorizacao = leituraEMemorizacao;
  }

  public String getPontosDeAtencao() {
    return pontosDeAtencao;
  }

  public void setPontosDeAtencao(String pontosDeAtencao) {
    this.pontosDeAtencao = pontosDeAtencao;
  }

  public String getEstrategiasPedagogicas() {
    return estrategiasPedagogicas;
  }

  public void setEstrategiasPedagogicas(String estrategiasPedagogicas) {
    this.estrategiasPedagogicas = estrategiasPedagogicas;
  }

  public String getAcompanhamentoFamiliar() {
    return acompanhamentoFamiliar;
  }

  public void setAcompanhamentoFamiliar(String acompanhamentoFamiliar) {
    this.acompanhamentoFamiliar = acompanhamentoFamiliar;
  }

  public String getPlanejamentoProximoSemestre() {
    return planejamentoProximoSemestre;
  }

  public void setPlanejamentoProximoSemestre(String planejamentoProximoSemestre) {
    this.planejamentoProximoSemestre = planejamentoProximoSemestre;
  }

  public String getParecerFinal() {
    return parecerFinal;
  }

  public void setParecerFinal(String parecerFinal) {
    this.parecerFinal = parecerFinal;
  }

  public int getVersao() {
    return versao;
  }

  public void setVersao(int versao) {
    this.versao = versao;
  }

  public StatusRelatorioSemestral getStatus() {
    return status;
  }

  public void setStatus(StatusRelatorioSemestral status) {
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

  /** Status possiveis de um RelatorioSemestral (FR-021). */
  public enum StatusRelatorioSemestral {
    PENDENTE_REVISAO,
    APROVADO
  }
}
