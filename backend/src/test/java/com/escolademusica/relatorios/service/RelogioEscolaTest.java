package com.escolademusica.relatorios.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/**
 * {@link RelogioEscola} deve sempre responder "hoje" no fuso configurado, nunca no fuso do servidor
 * onde o backend roda (o container roda em UTC).
 */
class RelogioEscolaTest {

  @Test
  void deveResponderHojeNoFusoConfigurado() {
    RelogioEscola relogio = new RelogioEscola("America/Sao_Paulo");

    assertThat(relogio.hoje()).isEqualTo(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
  }

  @Test
  void devePermitirFusosDiferentesParaOMesmoInstante() {
    RelogioEscola relogioSaoPaulo = new RelogioEscola("America/Sao_Paulo");
    RelogioEscola relogioUtc = new RelogioEscola("UTC");

    assertThat(relogioSaoPaulo.hoje()).isEqualTo(LocalDate.now(ZoneId.of("America/Sao_Paulo")));
    assertThat(relogioUtc.hoje()).isEqualTo(LocalDate.now(ZoneId.of("UTC")));
  }
}
