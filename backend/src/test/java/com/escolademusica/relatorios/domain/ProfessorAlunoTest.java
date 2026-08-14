package com.escolademusica.relatorios.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfessorAlunoTest {

  @Test
  void construtorPadraoDeveCriarInstanciaVazia() {
    ProfessorAluno vinculo = new ProfessorAluno();

    assertThat(vinculo.getProfessorId()).isNull();
    assertThat(vinculo.getAlunoId()).isNull();
  }

  @Test
  void construtorComArgumentosDevePreencherProfessorEAluno() {
    UUID professorId = UUID.randomUUID();
    UUID alunoId = UUID.randomUUID();

    ProfessorAluno vinculo = new ProfessorAluno(professorId, alunoId);

    assertThat(vinculo.getProfessorId()).isEqualTo(professorId);
    assertThat(vinculo.getAlunoId()).isEqualTo(alunoId);
  }

  @Test
  void deveExporTodosOsGettersESetters() {
    ProfessorAluno vinculo = new ProfessorAluno();
    UUID id = UUID.randomUUID();
    UUID professorId = UUID.randomUUID();
    UUID alunoId = UUID.randomUUID();
    OffsetDateTime agora = OffsetDateTime.now();

    vinculo.setId(id);
    vinculo.setProfessorId(professorId);
    vinculo.setAlunoId(alunoId);
    vinculo.setCriadoEm(agora);

    assertThat(vinculo.getId()).isEqualTo(id);
    assertThat(vinculo.getProfessorId()).isEqualTo(professorId);
    assertThat(vinculo.getAlunoId()).isEqualTo(alunoId);
    assertThat(vinculo.getCriadoEm()).isEqualTo(agora);
  }
}
