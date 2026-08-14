package com.escolademusica.relatorios.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Implementacao concreta de {@link SpeechToTextGateway} (T038) que chama um provedor de
 * Speech-to-Text generico via HTTP REST.
 *
 * <p>Como nenhum provedor especifico foi definido no research.md, esta implementacao adota um
 * contrato REST generico e configuravel (URL + chave via variaveis de ambiente), pensado para ser
 * facilmente adaptavel ao provedor real escolhido futuramente (ex.: OpenAI Whisper, Google
 * Speech-to-Text, AssemblyAI): envia os bytes do audio + formato, recebe um JSON de resposta com um
 * campo de transcricao.
 */
@Component
public class SpeechToTextGatewayImpl implements SpeechToTextGateway {

  private static final Logger log = LoggerFactory.getLogger(SpeechToTextGatewayImpl.class);

  private final RestClient restClient;
  private final String apiKey;
  private final ObjectMapper objectMapper;

  public SpeechToTextGatewayImpl(
      @Value("${stt.provider.url:https://stt-provider.example.com/v1/transcricoes}")
          String providerUrl,
      @Value("${stt.api-key}") String apiKey,
      ObjectMapper objectMapper) {
    this.restClient = RestClient.builder().baseUrl(providerUrl).build();
    this.apiKey = apiKey;
    this.objectMapper = objectMapper;
  }

  @Override
  public String transcrever(byte[] audioBytes, String formato) {
    if (audioBytes == null || audioBytes.length == 0) {
      throw new AudioNaoProcessavelException("Audio vazio ou ausente");
    }

    String respostaBruta;
    try {
      respostaBruta =
          restClient
              .post()
              .contentType(MediaType.APPLICATION_OCTET_STREAM)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
              .header("X-Audio-Format", formato == null ? "ogg" : formato)
              .body(audioBytes)
              .retrieve()
              .body(String.class);
    } catch (RestClientException e) {
      log.warn("Falha ao chamar provedor de Speech-to-Text", e);
      throw new AudioNaoProcessavelException(
          "Nao foi possivel transcrever o audio: falha no provedor de Speech-to-Text", e);
    }

    String transcricao = extrairTranscricao(respostaBruta);
    if (transcricao == null || transcricao.isBlank()) {
      throw new AudioNaoProcessavelException(
          "Audio nao pode ser transcrito (corrompido, silencio ou formato nao suportado)");
    }
    return transcricao;
  }

  /** Extrai o campo de transcricao de um JSON generico {@code { "transcricao": "..." } }. */
  private String extrairTranscricao(String respostaBruta) {
    if (respostaBruta == null || respostaBruta.isBlank()) {
      return null;
    }
    try {
      JsonNode raiz = objectMapper.readTree(respostaBruta);
      JsonNode campo = raiz.has("transcricao") ? raiz.get("transcricao") : raiz.get("text");
      return campo == null ? null : campo.asText(null);
    } catch (Exception e) {
      log.warn("Resposta do provedor de Speech-to-Text nao e um JSON valido", e);
      return null;
    }
  }
}
