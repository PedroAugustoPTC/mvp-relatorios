package com.escolademusica.relatorios.service;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.domain.Aula;
import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.dto.RelatorioAulaPdfConteudoDto;
import com.escolademusica.relatorios.mapper.RelatorioAulaMapper;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.repository.AulaRepository;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Helper compartilhado pelos use cases de relatorio de aula (Estruturar, ResponderPergunta,
 * Revisar) para montar o conteudo do PDF e acionar {@link PdfGeracaoService}, evitando duplicar a
 * busca de nomes de aluno/professor e data da aula em cada use case (T042/T043/T044).
 */
@Service
public class RelatorioAulaPdfService {

  private final AulaRepository aulaRepository;
  private final AlunoRepository alunoRepository;
  private final ProfessorRepository professorRepository;
  private final RelatorioAulaMapper mapper;
  private final PdfGeracaoService pdfGeracaoService;

  public RelatorioAulaPdfService(
      AulaRepository aulaRepository,
      AlunoRepository alunoRepository,
      ProfessorRepository professorRepository,
      RelatorioAulaMapper mapper,
      PdfGeracaoService pdfGeracaoService) {
    this.aulaRepository = aulaRepository;
    this.alunoRepository = alunoRepository;
    this.professorRepository = professorRepository;
    this.mapper = mapper;
    this.pdfGeracaoService = pdfGeracaoService;
  }

  /** Gera o PDF da versao atual de {@code relatorio} e atualiza seu {@code pdfUrl} em memoria. */
  public void gerarEAplicarPdf(RelatorioAula relatorio) {
    LocalDate dataAula =
        aulaRepository.findById(relatorio.getAulaId()).map(Aula::getDataAula).orElse(null);
    String nomeAluno =
        alunoRepository.findById(relatorio.getAlunoId()).map(Aluno::getNome).orElse("");
    String nomeProfessor =
        professorRepository.findById(relatorio.getProfessorId()).map(Professor::getNome).orElse("");

    RelatorioAulaPdfConteudoDto conteudo =
        mapper.paraConteudoPdf(relatorio, nomeAluno, nomeProfessor, dataAula);
    String pdfUrl =
        pdfGeracaoService.gerarPdfRelatorioAula(relatorio.getId(), relatorio.getVersao(), conteudo);
    relatorio.setPdfUrl(pdfUrl);
  }
}
