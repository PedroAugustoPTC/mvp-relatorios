package com.escolademusica.relatorios.gateway;

/**
 * Porta para envio de mensagens/arquivos ao professor via bot do Telegram. O bot em si e operado
 * pelo n8n (orquestracao do fluxo de conversa); este gateway cobre os casos em que o proprio
 * backend precisa notificar o professor diretamente (ex.: entrega do PDF gerado).
 */
public interface TelegramGateway {

  /**
   * Envia uma mensagem de texto simples.
   *
   * @param chatId identificador do chat/usuario do Telegram (telegramUserId)
   * @param texto conteudo da mensagem
   */
  void enviarMensagem(String chatId, String texto);

  /**
   * Envia um arquivo (ex.: PDF de relatorio) para o chat informado.
   *
   * @param chatId identificador do chat/usuario do Telegram (telegramUserId)
   * @param arquivo conteudo binario do arquivo
   * @param nomeArquivo nome do arquivo exibido ao usuario
   */
  void enviarArquivo(String chatId, byte[] arquivo, String nomeArquivo);
}
