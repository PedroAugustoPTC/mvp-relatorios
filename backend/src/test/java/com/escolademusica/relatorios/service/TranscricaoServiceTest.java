package com.escolademusica.relatorios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.gateway.SpeechToTextGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranscricaoServiceTest {

  @Mock private SpeechToTextGateway speechToTextGateway;

  private TranscricaoService transcricaoService;

  @BeforeEach
  void setUp() {
    transcricaoService = new TranscricaoService(speechToTextGateway);
  }

  @Test
  void transcreverDeveDelegarAoGatewayERetornarTexto() {
    byte[] audio = new byte[] {1, 2, 3};
    when(speechToTextGateway.transcrever(audio, "ogg")).thenReturn("texto transcrito");

    String resultado = transcricaoService.transcrever(audio, "ogg");

    assertThat(resultado).isEqualTo("texto transcrito");
    verify(speechToTextGateway).transcrever(audio, "ogg");
  }
}
