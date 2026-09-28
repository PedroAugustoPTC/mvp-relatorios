package com.escolademusica.relatorios.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fonte unica de "hoje" no fuso horario da escola ({@code escola.timezone}, default {@code
 * America/Sao_Paulo}) — nunca o fuso do servidor onde o backend roda.
 *
 * <p>O container do backend roda em UTC. Usar {@code LocalDate.now()} sem fuso explicito data a
 * aula/relatorio com o dia UTC: uma aula registrada as 21h de um dia em Brasilia (UTC-3) ja e meia-
 * noite UTC do dia seguinte, gravando a data errada (um dia a frente do que o professor via no
 * relogio dele).
 */
@Component
public class RelogioEscola {

  private final Clock clock;

  public RelogioEscola(@Value("${escola.timezone:America/Sao_Paulo}") String timezone) {
    this.clock = Clock.system(ZoneId.of(timezone));
  }

  public LocalDate hoje() {
    return LocalDate.now(clock);
  }
}
