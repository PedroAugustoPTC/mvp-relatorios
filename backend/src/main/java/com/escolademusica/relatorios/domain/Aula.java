package com.escolademusica.relatorios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Evento pontual de ensino entre um professor e um aluno. Multiplas Aulas podem existir para o
 * mesmo par (professor, aluno) na mesma dataAula -- cada uma gera seu proprio relatorio, sem
 * deduplicacao.
 */
@Entity
@Table(name = "aula")
public class Aula {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "professor_id", nullable = false)
  private UUID professorId;

  @Column(name = "aluno_id", nullable = false)
  private UUID alunoId;

  @Column(name = "data_aula", nullable = false)
  private LocalDate dataAula;

  @Column(name = "criado_em", nullable = false)
  private OffsetDateTime criadoEm;

  public Aula() {}

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

  public LocalDate getDataAula() {
    return dataAula;
  }

  public void setDataAula(LocalDate dataAula) {
    this.dataAula = dataAula;
  }

  public OffsetDateTime getCriadoEm() {
    return criadoEm;
  }

  public void setCriadoEm(OffsetDateTime criadoEm) {
    this.criadoEm = criadoEm;
  }
}
