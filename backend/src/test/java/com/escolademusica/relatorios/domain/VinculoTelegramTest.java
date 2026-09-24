package com.escolademusica.relatorios.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VinculoTelegramTest {

  @Test
  void deveExporTodosOsGettersESetters() {
    VinculoTelegram vinculo = new VinculoTelegram();
    UUID id = UUID.randomUUID();
    UUID professorId = UUID.randomUUID();
    OffsetDateTime agora = OffsetDateTime.now();

    UUID alunoSelecionadoId = UUID.randomUUID();

    vinculo.setId(id);
    vinculo.setProfessorId(professorId);
    vinculo.setTelegramUserId("123456");
    vinculo.setVinculadoEm(agora);
    vinculo.setAlunoSelecionadoId(alunoSelecionadoId);
    vinculo.setAlunoSelecionadoEm(agora);

    assertThat(vinculo.getId()).isEqualTo(id);
    assertThat(vinculo.getProfessorId()).isEqualTo(professorId);
    assertThat(vinculo.getTelegramUserId()).isEqualTo("123456");
    assertThat(vinculo.getVinculadoEm()).isEqualTo(agora);
    assertThat(vinculo.getAlunoSelecionadoId()).isEqualTo(alunoSelecionadoId);
    assertThat(vinculo.getAlunoSelecionadoEm()).isEqualTo(agora);
  }

  @Test
  void alunoSelecionadoDeveComecarNuloAntesDeQualquerSelecaoNoTelegram() {
    VinculoTelegram vinculo = new VinculoTelegram();

    assertThat(vinculo.getAlunoSelecionadoId()).isNull();
    assertThat(vinculo.getAlunoSelecionadoEm()).isNull();
  }
}
