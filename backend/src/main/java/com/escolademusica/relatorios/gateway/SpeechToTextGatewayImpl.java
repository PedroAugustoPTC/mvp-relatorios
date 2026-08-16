package com.escolademusica.relatorios.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Implementacao concreta de {@link SpeechToTextGateway} (T038) sobre a API de transcricao da Groq
 * (Whisper), que segue o mesmo contrato REST da API de audio da OpenAI: {@code POST
 * /openai/v1/audio/transcriptions} com corpo {@code multipart/form-data} contendo os campos {@code
 * file} e {@code model}, e resposta {@code { "text": "..." }}.
 *
 * <p>O modelo padrao ({@code whisper-large-v3-turbo}) aceita diretamente o {@code .webm}/Opus
 * gravado pelo navegador no portal do professor, portanto nenhuma conversao de formato e necessaria
 * antes do envio. Por ser compativel com o contrato da OpenAI, trocar para a propria OpenAI (ou
 * outro provedor compativel) exige apenas ajustar {@code STT_PROVIDER_URL} e {@code STT_MODEL}.
 */
@Component
public class SpeechToTextGatewayImpl implements SpeechToTextGateway {

  private static final Logger log = LoggerFactory.getLogger(SpeechToTextGatewayImpl.class);

  private final RestClient restClient;
  private final String apiKey;
  private final String modelo;
  private final String idioma;
  private final ObjectMapper objectMapper;

  public SpeechToTextGatewayImpl(
      @Value("${stt.provider.url:https://api.groq.com/openai/v1/audio/transcriptions}")
          String providerUrl,
      @Value("${stt.api-key}") String apiKey,
      @Value("${stt.model:whisper-large-v3-turbo}") String modelo,
      @Value("${stt.language:pt}") String idioma,
      ObjectMapper objectMapper) {
    this.restClient = RestClient.builder().baseUrl(providerUrl).build();
    this.apiKey = apiKey;
    this.modelo = modelo;
    this.idioma = idioma;
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
              .contentType(MediaType.MULTIPART_FORM_DATA)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
              .body(montarCorpo(audioBytes, formato))
              .retrieve()
              .body(String.class);
    } catch (RestClientResponseException e) {
      // O provedor respondeu com erro HTTP (ex.: 401 chave invalida, 413 audio grande demais,
      // 429 limite do plano gratuito): o corpo traz a causa e e essencial para diagnostico.
      log.warn(
          "Provedor de Speech-to-Text retornou HTTP {}: {}",
          e.getStatusCode(),
          e.getResponseBodyAsString(),
          e);
      throw new AudioNaoProcessavelException(
          "Nao foi possivel transcrever o audio: provedor de Speech-to-Text retornou HTTP "
              + e.getStatusCode(),
          e);
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

  /**
   * Monta o corpo multipart esperado pela API de transcricao (campos {@code file} e {@code model}).
   */
  private MultiValueMap<String, Object> montarCorpo(byte[] audioBytes, String formato) {
    String extensao = (formato == null || formato.isBlank()) ? "ogg" : formato;
    // O provedor identifica o container do audio pela extensao do nome do arquivo enviado, entao o
    // formato recebido do controller (derivado do upload do professor) vira o nome do arquivo.
    ByteArrayResource arquivo =
        new ByteArrayResource(audioBytes) {
          @Override
          public String getFilename() {
            return "audio." + extensao;
          }
        };

    MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
    corpo.add("file", arquivo);
    corpo.add("model", modelo);
    corpo.add("response_format", "json");
    if (idioma != null && !idioma.isBlank()) {
      corpo.add("language", idioma);
    }
    return corpo;
  }

  /** Extrai o campo de transcricao da resposta {@code { "text": "..." } }. */
  private String extrairTranscricao(String respostaBruta) {
    if (respostaBruta == null || respostaBruta.isBlank()) {
      return null;
    }
    try {
      JsonNode raiz = objectMapper.readTree(respostaBruta);
      JsonNode campo = raiz.has("text") ? raiz.get("text") : raiz.get("transcricao");
      return campo == null ? null : campo.asText(null);
    } catch (Exception e) {
      log.warn("Resposta do provedor de Speech-to-Text nao e um JSON valido", e);
      return null;
    }
  }
}
