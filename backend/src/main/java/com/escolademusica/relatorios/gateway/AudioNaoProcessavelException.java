package com.escolademusica.relatorios.gateway;

/**
 * Lancada por {@link SpeechToTextGateway} quando o audio recebido nao pode ser transcrito
 * (corrompido, vazio, formato nao suportado, ou falha irrecuperavel do provedor).
 */
public class AudioNaoProcessavelException extends RuntimeException {

  public AudioNaoProcessavelException(String mensagem) {
    super(mensagem);
  }

  public AudioNaoProcessavelException(String mensagem, Throwable causa) {
    super(mensagem, causa);
  }
}
