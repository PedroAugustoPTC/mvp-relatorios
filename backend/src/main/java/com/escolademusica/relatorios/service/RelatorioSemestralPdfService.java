package com.escolademusica.relatorios.service;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.dto.RelatorioSemestralPdfConteudoDto;
import com.escolademusica.relatorios.mapper.RelatorioSemestralMapper;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.usecase.support.PeriodoSemestralResolver;
import org.springframework.stereotype.Service;

/**
 * Helper compartilhado pelos use cases de relatorio semestral (Gerar, Revisar) para montar o
 * conteudo do PDF e acionar {@link PdfGeracaoService}, evitando duplicar a busca de nomes de
 * aluno/professor e do rotulo do periodo em cada use case (T074/T075/T078).
 */
@Service
public class RelatorioSemestralPdfService {

  private final AlunoRepository alunoRepository;
  private final ProfessorRepository professorRepository;
  private final RelatorioSemestralMapper mapper;
  private final PdfGeracaoService pdfGeracaoService;

  public RelatorioSemestralPdfService(
      AlunoRepository alunoRepository,
      ProfessorRepository professorRepository,
      RelatorioSemestralMapper mapper,
      PdfGeracaoService pdfGeracaoService) {
    this.alunoRepository = alunoRepository;
    this.professorRepository = professorRepository;
    this.mapper = mapper;
    this.pdfGeracaoService = pdfGeracaoService;
  }

  /** Gera o PDF da versao atual de {@code relatorio} e atualiza seu {@code pdfUrl} em memoria. */
  public void gerarEAplicarPdf(RelatorioSemestral relatorio) {
    String nomeAluno =
        alunoRepository.findById(relatorio.getAlunoId()).map(Aluno::getNome).orElse("");
    String nomeProfessor =
        professorRepository.findById(relatorio.getProfessorId()).map(Professor::getNome).orElse("");
    String rotuloPeriodo = rotularPeriodo(relatorio);

    RelatorioSemestralPdfConteudoDto conteudo =
        mapper.paraConteudoPdf(relatorio, nomeAluno, nomeProfessor, rotuloPeriodo);
    String pdfUrl =
        pdfGeracaoService.gerarPdfRelatorioSemestral(
            relatorio.getId(), relatorio.getVersao(), conteudo);
    relatorio.setPdfUrl(pdfUrl);
  }

  private String rotularPeriodo(RelatorioSemestral relatorio) {
    int ano = relatorio.getPeriodoInicio().getYear();
    int semestre = relatorio.getPeriodoInicio().getMonthValue() <= 6 ? 1 : 2;
    return PeriodoSemestralResolver.resolver(ano, semestre).rotulo();
  }
}
