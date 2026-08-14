package com.escolademusica.relatorios.gateway;

/**
 * Porta para o provedor de Speech-to-Text (research.md secao 1). A implementacao concreta e um
 * detalhe de infraestrutura, acionada por um endpoint interno chamado pelo workflow do n8n — nunca
 * diretamente pelo n8n contra a API do provedor.
 */
public interface SpeechToTextGateway {

  /**
   * Transcreve o audio de uma aula para texto.
   *
   * @param audioBytes conteudo binario do arquivo de audio
   * @param formato formato/extensao do audio (ex.: "ogg", "mp3"), usado pelo provedor para
   *     decodificacao
   * @return texto transcrito
   * @throws AudioNaoProcessavelException se o audio estiver corrompido, vazio, em formato nao
   *     suportado, ou o provedor nao conseguir gerar uma transcricao utilizavel
   */
  String transcrever(byte[] audioBytes, String formato);
}
