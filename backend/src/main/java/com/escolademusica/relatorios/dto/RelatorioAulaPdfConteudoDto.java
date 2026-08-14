package com.escolademusica.relatorios.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Conteudo estruturado de um relatorio de aula pronto para renderizacao em PDF (T046), usado por
 * {@code PdfGeracaoService.renderPdfBytes}.
 */
public record RelatorioAulaPdfConteudoDto(
    String nomeAluno,
    String nomeProfessor,
    LocalDate dataAula,
    List<String> conteudosTrabalhados,
    String evolucao,
    List<String> dificuldades,
    List<String> atividadesPropostas,
    String observacoes) {}
