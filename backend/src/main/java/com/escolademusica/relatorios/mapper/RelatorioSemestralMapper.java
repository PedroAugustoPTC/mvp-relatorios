package com.escolademusica.relatorios.mapper;

import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.dto.RelatorioSemestralLlmPayloadDto;
import com.escolademusica.relatorios.dto.RelatorioSemestralPdfConteudoDto;
import com.escolademusica.relatorios.dto.RelatorioSemestralResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Converte entre o payload JSON trocado com o {@link
 * com.escolademusica.relatorios.gateway.LlmGateway}, os campos JSON persistidos em {@link
 * RelatorioSemestral} (colunas {@code jsonb}/{@code text}) e os DTOs de resposta da API (T080).
 */
@Component
public class RelatorioSemestralMapper {

  private final ObjectMapper objectMapper;

  public RelatorioSemestralMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Desserializa o JSON textual retornado pelo LlmGateway. */
  public RelatorioSemestralLlmPayloadDto lerPayloadLlm(String json) {
    try {
      return objectMapper.readValue(json, RelatorioSemestralLlmPayloadDto.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "JSON estruturado do LLM invalido para RelatorioSemestral", e);
    }
  }

  /** Aplica os campos de {@code payload} sobre a entidade (serializando cada secao para JSON). */
  public void aplicarPayload(RelatorioSemestral entidade, RelatorioSemestralLlmPayloadDto payload) {
    entidade.setInformacoesGerais(escreverJson(payload.informacoesGerais()));
    entidade.setFrequenciaEEstudo(escreverJson(payload.frequenciaEEstudo()));
    entidade.setTecnica(escreverJson(payload.tecnica()));
    entidade.setMusicalidade(escreverJson(payload.musicalidade()));
    entidade.setLeituraEMemorizacao(escreverJson(payload.leituraEMemorizacao()));
    entidade.setPontosDeAtencao(escreverJson(payload.pontosDeAtencao()));
    entidade.setEstrategiasPedagogicas(escreverJson(payload.estrategiasPedagogicas()));
    entidade.setAcompanhamentoFamiliar(escreverJson(payload.acompanhamentoFamiliar()));
    entidade.setPlanejamentoProximoSemestre(escreverJson(payload.planejamentoProximoSemestre()));
    entidade.setParecerFinal(payload.parecerFinal());
  }

  /** Monta o payload atual da entidade, para reenviar ao LlmGateway em uma revisao (T075). */
  public RelatorioSemestralLlmPayloadDto paraPayload(RelatorioSemestral entidade) {
    return new RelatorioSemestralLlmPayloadDto(
        lerJson(entidade.getInformacoesGerais()),
        lerJson(entidade.getFrequenciaEEstudo()),
        lerJson(entidade.getTecnica()),
        lerJson(entidade.getMusicalidade()),
        lerJson(entidade.getLeituraEMemorizacao()),
        lerJson(entidade.getPontosDeAtencao()),
        lerJson(entidade.getEstrategiasPedagogicas()),
        lerJson(entidade.getAcompanhamentoFamiliar()),
        lerJson(entidade.getPlanejamentoProximoSemestre()),
        entidade.getParecerFinal());
  }

  public String escreverJsonPayload(RelatorioSemestralLlmPayloadDto payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Falha ao serializar payload de RelatorioSemestral", e);
    }
  }

  /** Constroi o DTO de resposta comum aos endpoints de gerar/revisar. */
  public RelatorioSemestralResponseDto paraResponseDto(RelatorioSemestral entidade) {
    return new RelatorioSemestralResponseDto(
        entidade.getId(),
        entidade.getStatus().name(),
        entidade.getQuantidadeRelatoriosAulaConsiderados(),
        lerJson(entidade.getInformacoesGerais()),
        lerJson(entidade.getFrequenciaEEstudo()),
        lerJson(entidade.getTecnica()),
        lerJson(entidade.getMusicalidade()),
        lerJson(entidade.getLeituraEMemorizacao()),
        lerJson(entidade.getPontosDeAtencao()),
        lerJson(entidade.getEstrategiasPedagogicas()),
        lerJson(entidade.getAcompanhamentoFamiliar()),
        lerJson(entidade.getPlanejamentoProximoSemestre()),
        entidade.getParecerFinal(),
        entidade.getPdfUrl(),
        entidade.getVersao());
  }

  /** Monta o conteudo estruturado pronto para renderizacao em PDF (T078). */
  public RelatorioSemestralPdfConteudoDto paraConteudoPdf(
      RelatorioSemestral entidade, String nomeAluno, String nomeProfessor, String rotuloPeriodo) {
    LocalDate inicio = entidade.getPeriodoInicio();
    LocalDate fim = entidade.getPeriodoFim();
    return new RelatorioSemestralPdfConteudoDto(
        nomeAluno,
        nomeProfessor,
        rotuloPeriodo,
        inicio,
        fim,
        entidade.getQuantidadeRelatoriosAulaConsiderados(),
        lerJson(entidade.getInformacoesGerais()),
        lerJson(entidade.getFrequenciaEEstudo()),
        lerJson(entidade.getTecnica()),
        lerJson(entidade.getMusicalidade()),
        lerJson(entidade.getLeituraEMemorizacao()),
        lerJson(entidade.getPontosDeAtencao()),
        lerJson(entidade.getEstrategiasPedagogicas()),
        lerJson(entidade.getAcompanhamentoFamiliar()),
        lerJson(entidade.getPlanejamentoProximoSemestre()),
        entidade.getParecerFinal());
  }

  private String escreverJson(JsonNode node) {
    try {
      return objectMapper.writeValueAsString(node == null ? NullNode.getInstance() : node);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Falha ao serializar secao do relatorio semestral", e);
    }
  }

  private JsonNode lerJson(String json) {
    if (json == null || json.isBlank()) {
      return NullNode.getInstance();
    }
    try {
      return objectMapper.readTree(json);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Falha ao desserializar secao JSON persistida", e);
    }
  }
}
