package com.escolademusica.relatorios.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Implementacao concreta de {@link LlmGateway} (T039) que chama um provedor de LLM generico via
 * HTTP REST, enviando um prompt de estruturacao/revisao e retornando o JSON textual da resposta.
 *
 * <p>Como nenhum provedor especifico foi definido no research.md, esta implementacao adota um
 * contrato REST generico (URL + chave via variaveis de ambiente): envia {@code { "prompt": "..." }
 * } e espera receber {@code { "resposta": "...json..." } } (ou {@code "text"}/{@code "content"}). O
 * JSON retornado e validado sintaticamente antes de ser devolvido ao chamador; se invalido, uma
 * unica tentativa de correcao e feita reenviando um prompt pedindo para corrigir o JSON (Edge Cases
 * do spec.md).
 */
@Component
public class LlmGatewayImpl implements LlmGateway {

  private static final Logger log = LoggerFactory.getLogger(LlmGatewayImpl.class);

  private static final String PROMPT_ESTRUTURACAO =
      """
      Voce e um assistente que estrutura relatorios de aulas de musica a partir da transcricao \
      de um professor. Retorne APENAS um JSON valido (sem markdown, sem comentarios) com \
      exatamente os campos:
      {
        "conteudosTrabalhados": ["..."],
        "evolucao": "...",
        "dificuldades": ["..."],
        "atividadesPropostas": ["..."],
        "observacoes": "...",
        "perguntasPendentes": ["..."]
      }
      Preencha "perguntasPendentes" apenas com perguntas objetivas de acompanhamento quando uma \
      informacao relevante nao puder ser extraida com confianca da transcricao (ex.: tarefa de \
      casa nao mencionada). Se toda a informacao necessaria estiver presente, retorne \
      "perguntasPendentes": [].

      Transcricao da aula:
      %s
      """;

  private static final String PROMPT_REVISAO =
      """
      Voce recebeu um relatorio de aula de musica ja estruturado em JSON e uma instrucao de \
      alteracao em linguagem natural do professor. Aplique a instrucao e retorne APENAS o JSON \
      atualizado, com os mesmos campos do relatorio original (conteudosTrabalhados, evolucao, \
      dificuldades, atividadesPropostas, observacoes, perguntasPendentes), sem markdown e sem \
      comentarios.

      Relatorio atual (JSON):
      %s

      Instrucao de alteracao:
      %s
      """;

  private static final String PROMPT_CONSOLIDACAO_SEMESTRAL =
      """
      Voce e um assistente pedagogico de uma escola de musica. Consolide a lista de relatorios de \
      aula (em JSON) informada abaixo em um UNICO relatorio semestral, seguindo o modelo de \
      referencia da escola. Retorne APENAS um JSON valido (sem markdown, sem comentarios) com \
      exatamente os campos:
      {
        "informacoesGerais": { "nome": "...", "idade": "...", "nivel": "...", "professor": "...", \
      "periodo": "..." },
        "frequenciaEEstudo": { "frequenciaAulas": "...", "regularidadeEstudoEmCasa": "...", \
      "engajamento": "...", "comentarioGeral": "..." },
        "tecnica": { "conteudosTrabalhados": ["..."], "evolucaoTecnica": "...", \
      "pontosADesenvolver": ["..."], "estudosRealizados": ["..."] },
        "musicalidade": { "repertorioEstudado": ["..."], "som": "...", "ritmo": "...", \
      "fraseado": "...", "articulacao": "...", "estilo": "...", "comentarioMusical": "..." },
        "leituraEMemorizacao": { "avaliacao": "...", "comentario": "..." },
        "pontosDeAtencao": { "itens": ["..."] },
        "estrategiasPedagogicas": { "itens": ["..."] },
        "acompanhamentoFamiliar": { "nivelDePresenca": "...", "observacaoDoProfessor": "..." },
        "planejamentoProximoSemestre": { "metasTecnicas": ["..."], "metasMusicais": ["..."], \
      "habitosDeEstudo": ["..."], "repertorioProposto": ["..."], "estudosTecnicos": ["..."], \
      "leituraETeoria": ["..."], "projetoDoSemestre": "..." },
        "parecerFinal": "..."
      }
      Baseie-se exclusivamente nas informacoes contidas nos relatorios de aula fornecidos; nao \
      invente dados que nao possam ser inferidos deles.

      Relatorios de aula do periodo (um JSON por linha):
      %s
      """;

  private static final String PROMPT_REVISAO_SEMESTRAL =
      """
      Voce recebeu um relatorio semestral de musica ja consolidado em JSON e uma instrucao de \
      alteracao em linguagem natural do professor. Aplique a instrucao e retorne APENAS o JSON \
      atualizado, com os mesmos campos do relatorio original (informacoesGerais, \
      frequenciaEEstudo, tecnica, musicalidade, leituraEMemorizacao, pontosDeAtencao, \
      estrategiasPedagogicas, acompanhamentoFamiliar, planejamentoProximoSemestre, parecerFinal), \
      sem markdown e sem comentarios.

      Relatorio semestral atual (JSON):
      %s

      Instrucao de alteracao:
      %s
      """;

  private static final String PROMPT_CORRIGIR_JSON =
      """
      A resposta abaixo deveria ser um JSON valido, mas nao pode ser interpretada. Corrija e \
      retorne APENAS o JSON valido, sem markdown e sem comentarios:

      %s
      """;

  private final RestClient restClient;
  private final String apiKey;
  private final ObjectMapper objectMapper;

  public LlmGatewayImpl(
      @Value("${llm.provider.url:https://llm-provider.example.com/v1/completions}")
          String providerUrl,
      @Value("${llm.api-key}") String apiKey,
      ObjectMapper objectMapper) {
    this.restClient = RestClient.builder().baseUrl(providerUrl).build();
    this.apiKey = apiKey;
    this.objectMapper = objectMapper;
  }

  @Override
  public String estruturarRelatorioAula(String transcricao) {
    String prompt = PROMPT_ESTRUTURACAO.formatted(transcricao == null ? "" : transcricao);
    return chamarComValidacaoJson(prompt);
  }

  @Override
  public String revisarRelatorioAula(String relatorioAtualJson, String instrucao) {
    String prompt =
        PROMPT_REVISAO.formatted(
            relatorioAtualJson == null ? "{}" : relatorioAtualJson,
            instrucao == null ? "" : instrucao);
    return chamarComValidacaoJson(prompt);
  }

  @Override
  public String consolidarRelatorioSemestral(List<String> relatoriosAulaJson) {
    String relatorios =
        relatoriosAulaJson == null || relatoriosAulaJson.isEmpty()
            ? ""
            : String.join("\n", relatoriosAulaJson);
    String prompt = PROMPT_CONSOLIDACAO_SEMESTRAL.formatted(relatorios);
    return chamarComValidacaoJson(prompt);
  }

  @Override
  public String revisarRelatorioSemestral(String relatorioAtualJson, String instrucao) {
    String prompt =
        PROMPT_REVISAO_SEMESTRAL.formatted(
            relatorioAtualJson == null ? "{}" : relatorioAtualJson,
            instrucao == null ? "" : instrucao);
    return chamarComValidacaoJson(prompt);
  }

  /** Chama o provedor e garante que a resposta e um JSON sintaticamente valido, com 1 retry. */
  private String chamarComValidacaoJson(String prompt) {
    String resposta = chamarProvedor(prompt);
    if (isJsonValido(resposta)) {
      return resposta;
    }
    log.warn("Resposta do LLM nao e um JSON valido; tentando corrigir com um novo prompt");
    String respostaCorrigida = chamarProvedor(PROMPT_CORRIGIR_JSON.formatted(resposta));
    if (isJsonValido(respostaCorrigida)) {
      return respostaCorrigida;
    }
    throw new IllegalStateException(
        "Provedor de LLM nao retornou um JSON valido apos tentativa de correcao");
  }

  private boolean isJsonValido(String texto) {
    if (texto == null || texto.isBlank()) {
      return false;
    }
    try {
      objectMapper.readTree(texto);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private String chamarProvedor(String prompt) {
    try {
      ObjectNode corpo = objectMapper.createObjectNode();
      corpo.put("prompt", prompt);
      String respostaBruta =
          restClient
              .post()
              .contentType(MediaType.APPLICATION_JSON)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
              .body(corpo)
              .retrieve()
              .body(String.class);
      return extrairConteudo(respostaBruta);
    } catch (RestClientException e) {
      log.warn("Falha ao chamar provedor de LLM", e);
      throw new IllegalStateException("Falha ao chamar o provedor de LLM", e);
    }
  }

  /**
   * Extrai o texto de conteudo de um JSON generico {@code { "resposta"|"text"|"content": ... } }.
   */
  private String extrairConteudo(String respostaBruta) {
    if (respostaBruta == null || respostaBruta.isBlank()) {
      return null;
    }
    try {
      JsonNode raiz = objectMapper.readTree(respostaBruta);
      for (String campo : List.of("resposta", "text", "content")) {
        JsonNode valor = raiz.get(campo);
        if (valor != null && !valor.isNull()) {
          return valor.asText(null);
        }
      }
      // Se o provedor ja retorna o JSON estruturado diretamente na raiz (sem envelope), devolve
      // o corpo bruto tal como recebido.
      return respostaBruta;
    } catch (Exception e) {
      // Corpo nao e um JSON de envelope reconhecido; assume que a resposta bruta ja e o
      // conteudo esperado (ex.: provedor retorna o JSON estruturado diretamente).
      return respostaBruta;
    }
  }
}
