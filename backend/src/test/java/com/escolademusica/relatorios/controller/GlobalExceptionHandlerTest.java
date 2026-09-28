package com.escolademusica.relatorios.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.escolademusica.relatorios.dto.ErroDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * {@link GlobalExceptionHandler#tratarAudioMuitoGrande} — audio de aula acima do limite configurado
 * (spring.servlet.multipart) deve virar um erro de negocio (413 + {@link ErroDto}), nunca a pagina
 * de erro HTML padrao do servlet container.
 */
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void deveResponder413ComCodigoDeNegocioQuandoAudioExcedeOLimite() {
    MaxUploadSizeExceededException excecao = new MaxUploadSizeExceededException(50 * 1024 * 1024);

    ResponseEntity<ErroDto> resposta = handler.tratarAudioMuitoGrande(excecao);

    assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    assertThat(resposta.getBody()).isNotNull();
    assertThat(resposta.getBody().codigo()).isEqualTo("AUDIO_MUITO_GRANDE");
  }
}
