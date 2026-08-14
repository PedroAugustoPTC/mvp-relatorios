package com.escolademusica.relatorios.mapper;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.RelatorioAulaLlmPayloadDto;
import com.escolademusica.relatorios.dto.RelatorioAulaPdfConteudoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Converte entre o payload JSON trocado com o {@link
 * com.escolademusica.relatorios.gateway.LlmGateway}, os campos JSON persistidos em {@link
 * RelatorioAula} (colunas {@code jsonb}/{@code text}) e os DTOs de resposta da API (T049).
 */
@Component
public class RelatorioAulaMapper {

  private final ObjectMapper objectMapper;

  public RelatorioAulaMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Desserializa o JSON textual retornado pelo LlmGateway. */
  public RelatorioAulaLlmPayloadDto lerPayloadLlm(String json) {
    try {
      return objectMapper.readValue(json, RelatorioAulaLlmPayloadDto.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("JSON estruturado do LLM invalido para RelatorioAula", e);
    }
  }

  /** Aplica os campos de {@code payload} sobre a entidade (serializando listas para JSON). */
  public void aplicarPayload(RelatorioAula entidade, RelatorioAulaLlmPayloadDto payload) {
    entidade.setConteudosTrabalhados(escreverJson(payload.conteudosTrabalhados()));
    entidade.setEvolucao(payload.evolucao());
    entidade.setDificuldades(escreverJson(payload.dificuldades()));
    entidade.setAtividadesPropostas(escreverJson(payload.atividadesPropostas()));
    entidade.setObservacoes(payload.observacoes());
  }

  /** Monta o payload atual da entidade, para reenviar ao LlmGateway em uma revisao (T044). */
  public RelatorioAulaLlmPayloadDto paraPayload(
      RelatorioAula entidade, List<String> perguntasPendentes) {
    return new RelatorioAulaLlmPayloadDto(
        lerLista(entidade.getConteudosTrabalhados()),
        entidade.getEvolucao(),
        lerLista(entidade.getDificuldades()),
        lerLista(entidade.getAtividadesPropostas()),
        entidade.getObservacoes(),
        perguntasPendentes);
  }

  public String escreverJsonPayload(RelatorioAulaLlmPayloadDto payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Falha ao serializar payload de RelatorioAula", e);
    }
  }

  /** Constroi o DTO de resposta comum aos endpoints de estruturar/responder-pergunta/revisar. */
  public EstruturarRelatorioResponseDto paraResponseDto(
      RelatorioAula entidade, List<String> perguntasPendentes) {
    return new EstruturarRelatorioResponseDto(
        entidade.getId(),
        entidade.getStatus().name(),
        lerLista(entidade.getConteudosTrabalhados()),
        entidade.getEvolucao(),
        lerLista(entidade.getDificuldades()),
        lerLista(entidade.getAtividadesPropostas()),
        entidade.getObservacoes(),
        perguntasPendentes == null ? List.of() : perguntasPendentes,
        entidade.getPdfUrl(),
        entidade.getVersao());
  }

  /** Monta o conteudo estruturado pronto para renderizacao em PDF (T046). */
  public RelatorioAulaPdfConteudoDto paraConteudoPdf(
      RelatorioAula entidade,
      String nomeAluno,
      String nomeProfessor,
      java.time.LocalDate dataAula) {
    return new RelatorioAulaPdfConteudoDto(
        nomeAluno,
        nomeProfessor,
        dataAula,
        lerLista(entidade.getConteudosTrabalhados()),
        entidade.getEvolucao(),
        lerLista(entidade.getDificuldades()),
        lerLista(entidade.getAtividadesPropostas()),
        entidade.getObservacoes());
  }

  private String escreverJson(List<String> lista) {
    try {
      return objectMapper.writeValueAsString(lista == null ? List.of() : lista);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Falha ao serializar lista para JSON", e);
    }
  }

  private List<String> lerLista(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<String>>() {});
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Falha ao desserializar lista JSON persistida", e);
    }
  }
}
