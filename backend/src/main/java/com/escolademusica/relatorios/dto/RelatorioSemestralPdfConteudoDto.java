package com.escolademusica.relatorios.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;

/**
 * Conteudo estruturado de um relatorio semestral pronto para renderizacao em PDF (T078), usado por
 * {@code PdfGeracaoService.renderPdfBytes}.
 */
public record RelatorioSemestralPdfConteudoDto(
    String nomeAluno,
    String nomeProfessor,
    String rotuloPeriodo,
    LocalDate periodoInicio,
    LocalDate periodoFim,
    int quantidadeRelatoriosAulaConsiderados,
    JsonNode informacoesGerais,
    JsonNode frequenciaEEstudo,
    JsonNode tecnica,
    JsonNode musicalidade,
    JsonNode leituraEMemorizacao,
    JsonNode pontosDeAtencao,
    JsonNode estrategiasPedagogicas,
    JsonNode acompanhamentoFamiliar,
    JsonNode planejamentoProximoSemestre,
    String parecerFinal) {}
