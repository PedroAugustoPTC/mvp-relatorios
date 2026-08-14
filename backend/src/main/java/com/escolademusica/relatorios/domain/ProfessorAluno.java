package com.escolademusica.relatorios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Vinculo de responsabilidade pedagogica entre professor e aluno, usado para controle de acesso
 * (FR-018) e para a lista de selecao do bot (FR-004).
 */
@Entity
@Table(
    name = "professor_aluno",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_professor_aluno",
          columnNames = {"professor_id", "aluno_id"})
    })
public class ProfessorAluno {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "professor_id", nullable = false)
  private UUID professorId;

  @Column(name = "aluno_id", nullable = false)
  private UUID alunoId;

  @Column(name = "criado_em", nullable = false)
  private OffsetDateTime criadoEm;

  public ProfessorAluno() {}

  public ProfessorAluno(UUID professorId, UUID alunoId) {
    this.professorId = professorId;
    this.alunoId = alunoId;
  }

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

  public UUID getAlunoId() {
    return alunoId;
  }

  public void setAlunoId(UUID alunoId) {
    this.alunoId = alunoId;
  }

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public void setCriadoEm(OffsetDateTime criadoEm) {
    this.criadoEm = criadoEm;
  }
}
