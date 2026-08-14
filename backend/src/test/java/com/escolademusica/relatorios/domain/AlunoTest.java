package com.escolademusica.relatorios.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlunoTest {

  @Test
  void deveConsiderarMenorDeIdadeQuandoAbaixoDoLimite() {
    Aluno aluno = new Aluno();
    aluno.setDataNascimento(LocalDate.of(2015, 1, 1));

    assertThat(aluno.isMenorDeIdadeEm(LocalDate.of(2026, 1, 1))).isTrue();
  }

  @Test
  void deveConsiderarMaiorDeIdadeQuandoNoOuAcimaDoLimite() {
    Aluno aluno = new Aluno();
    aluno.setDataNascimento(LocalDate.of(2000, 1, 1));

    assertThat(aluno.isMenorDeIdadeEm(LocalDate.of(2026, 1, 1))).isFalse();
  }

  @Test
  void deveRetornarFalsoQuandoDataNascimentoNula() {
    Aluno aluno = new Aluno();

    assertThat(aluno.isMenorDeIdadeEm(LocalDate.now())).isFalse();
  }

  @Test
  void deveRetornarFalsoQuandoDataReferenciaNula() {
    Aluno aluno = new Aluno();
    aluno.setDataNascimento(LocalDate.of(2015, 1, 1));

    assertThat(aluno.isMenorDeIdadeEm(null)).isFalse();
  }

  @Test
  void isMenorDeIdadeDeveUsarDataAtual() {
    Aluno aluno = new Aluno();
    aluno.setDataNascimento(LocalDate.now().minusYears(5));

    assertThat(aluno.isMenorDeIdade()).isTrue();
  }

  @Test
  void deveExporTodosOsGettersESetters() {
    Aluno aluno = new Aluno();
    UUID id = UUID.randomUUID();
    OffsetDateTime agora = OffsetDateTime.now();

    aluno.setId(id);
    aluno.setNome("Joana");
    aluno.setDataNascimento(LocalDate.of(1990, 5, 10));
    aluno.setCpfCriptografado(new byte[] {1, 2, 3});
    aluno.setCpfHash("hash");
    aluno.setNomeResponsavel("Responsavel");
    aluno.setAtivo(false);
    aluno.setCriadoEm(agora);
    aluno.setAtualizadoEm(agora);

    assertThat(aluno.getId()).isEqualTo(id);
    assertThat(aluno.getNome()).isEqualTo("Joana");
    assertThat(aluno.getDataNascimento()).isEqualTo(LocalDate.of(1990, 5, 10));
    assertThat(aluno.getCpfCriptografado()).containsExactly(1, 2, 3);
    assertThat(aluno.getCpfHash()).isEqualTo("hash");
    assertThat(aluno.getNomeResponsavel()).isEqualTo("Responsavel");
    assertThat(aluno.isAtivo()).isFalse();
    assertThat(aluno.getCriadoEm()).isEqualTo(agora);
    assertThat(aluno.getAtualizadoEm()).isEqualTo(agora);
  }
}
