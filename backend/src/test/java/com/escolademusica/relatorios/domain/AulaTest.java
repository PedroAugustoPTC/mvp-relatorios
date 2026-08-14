package com.escolademusica.relatorios.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AulaTest {

  @Test
  void deveExporTodosOsGettersESetters() {
    Aula aula = new Aula();
    UUID id = UUID.randomUUID();
    UUID professorId = UUID.randomUUID();
    UUID alunoId = UUID.randomUUID();
    LocalDate dataAula = LocalDate.of(2026, 3, 10);
    OffsetDateTime agora = OffsetDateTime.now();

    aula.setId(id);
    aula.setProfessorId(professorId);
    aula.setAlunoId(alunoId);
    aula.setDataAula(dataAula);
    aula.setCriadoEm(agora);

    assertThat(aula.getId()).isEqualTo(id);
    assertThat(aula.getProfessorId()).isEqualTo(professorId);
    assertThat(aula.getAlunoId()).isEqualTo(alunoId);
    assertThat(aula.getDataAula()).isEqualTo(dataAula);
    assertThat(aula.getCriadoEm()).isEqualTo(agora);
  }
}
