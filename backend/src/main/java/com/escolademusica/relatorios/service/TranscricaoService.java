package com.escolademusica.relatorios.service;

import com.escolademusica.relatorios.gateway.SpeechToTextGateway;
import org.springframework.stereotype.Service;

/**
 * Orquestra a transcricao de um audio de aula (T040), delegando ao {@link SpeechToTextGateway}.
 *
 * <p><b>FR-005a (exclusao imediata do audio temporario):</b> este servico nunca grava o audio em
 * disco. Os bytes recebidos chegam ao controller como parte do corpo multipart da requisicao HTTP
 * (gerenciado pelo Spring/Servlet container), sao mantidos apenas em memoria (array de bytes
 * transiente) durante a chamada a este metodo, e sao descartados/coletados pelo garbage collector
 * assim que a requisicao termina — nao ha arquivo temporario em disco a ser explicitamente apagado.
 * Caso uma implementacao futura de {@link SpeechToTextGateway} precise gravar um arquivo temporario
 * (ex.: SDK de provedor que exige um File), a exclusao deve ser feita em um bloco {@code finally}
 * imediatamente apos a chamada ao provedor, para preservar esta garantia.
 */
@Service
public class TranscricaoService {

  private final SpeechToTextGateway speechToTextGateway;

  public TranscricaoService(SpeechToTextGateway speechToTextGateway) {
    this.speechToTextGateway = speechToTextGateway;
  }

  /**
   * Transcreve o audio de uma aula.
   *
   * @param audioBytes conteudo binario do audio (mantido apenas em memoria)
   * @param formato formato/extensao do audio (ex.: "ogg")
   * @return texto transcrito
   * @throws com.escolademusica.relatorios.gateway.AudioNaoProcessavelException se o audio nao puder
   *     ser transcrito
   */
  public String transcrever(byte[] audioBytes, String formato) {
    return speechToTextGateway.transcrever(audioBytes, formato);
  }
}
