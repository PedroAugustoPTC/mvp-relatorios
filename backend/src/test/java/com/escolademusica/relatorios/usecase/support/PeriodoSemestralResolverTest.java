package com.escolademusica.relatorios.usecase.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PeriodoSemestralResolverTest {

  @Test
  void resolverDevePreencherPrimeiroSemestre() {
    PeriodoSemestralResolver.Periodo periodo = PeriodoSemestralResolver.resolver(2026, 1);

    assertThat(periodo.chave()).isEqualTo("2026-1");
    assertThat(periodo.rotulo()).isEqualTo("1º semestre 2026");
    assertThat(periodo.inicio()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(periodo.fim()).isEqualTo(LocalDate.of(2026, 6, 30));
  }

  @Test
  void resolverDevePreencherSegundoSemestre() {
    PeriodoSemestralResolver.Periodo periodo = PeriodoSemestralResolver.resolver(2026, 2);

    assertThat(periodo.chave()).isEqualTo("2026-2");
    assertThat(periodo.rotulo()).isEqualTo("2º semestre 2026");
    assertThat(periodo.inicio()).isEqualTo(LocalDate.of(2026, 7, 1));
    assertThat(periodo.fim()).isEqualTo(LocalDate.of(2026, 12, 31));
  }

  @Test
  void resolverDeveLancarQuandoSemestreInvalido() {
    assertThatThrownBy(() -> PeriodoSemestralResolver.resolver(2026, 3))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void resolverPorChaveDeveDelegarParaResolver() {
    PeriodoSemestralResolver.Periodo periodo = PeriodoSemestralResolver.resolverPorChave("2026-1");

    assertThat(periodo.chave()).isEqualTo("2026-1");
  }

  @Test
  void resolverPorChaveDeveLancarQuandoChaveNulaOuEmBranco() {
    assertThatThrownBy(() -> PeriodoSemestralResolver.resolverPorChave(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PeriodoSemestralResolver.resolverPorChave("  "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void resolverPorChaveDeveLancarQuandoFormatoInvalido() {
    assertThatThrownBy(() -> PeriodoSemestralResolver.resolverPorChave("2026"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PeriodoSemestralResolver.resolverPorChave("2026-1-extra"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void resolverPorChaveDeveLancarQuandoNaoNumerico() {
    assertThatThrownBy(() -> PeriodoSemestralResolver.resolverPorChave("AAAA-S"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
