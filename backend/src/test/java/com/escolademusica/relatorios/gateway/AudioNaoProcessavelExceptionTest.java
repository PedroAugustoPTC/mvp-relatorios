package com.escolademusica.relatorios.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AudioNaoProcessavelExceptionTest {

  @Test
  void construtorComMensagemDevePreencherMensagem() {
    AudioNaoProcessavelException excecao = new AudioNaoProcessavelException("audio invalido");

    assertThat(excecao.getMessage()).isEqualTo("audio invalido");
    assertThat(excecao.getCause()).isNull();
  }

  @Test
  void construtorComMensagemECausaDevePreencherAmbos() {
    RuntimeException causa = new RuntimeException("causa raiz");

    AudioNaoProcessavelException excecao =
        new AudioNaoProcessavelException("audio invalido", causa);

    assertThat(excecao.getMessage()).isEqualTo("audio invalido");
    assertThat(excecao.getCause()).isEqualTo(causa);
  }
}
