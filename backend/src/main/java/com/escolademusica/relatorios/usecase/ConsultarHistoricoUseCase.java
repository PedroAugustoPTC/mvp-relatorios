package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.domain.Aula;
import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioAula.StatusRelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.RelatorioSemestral.StatusRelatorioSemestral;
import com.escolademusica.relatorios.domain.exception.AlunoNaoAssociadoException;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.AlunoResumoDto;
import com.escolademusica.relatorios.dto.HistoricoAlunoResponseDto;
import com.escolademusica.relatorios.dto.ItemHistoricoDto;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.repository.AulaRepository;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Monta o historico cronologico (relatorios de aula + semestrais, ambos apenas {@code APROVADO}) de
 * um aluno (FR-017, FR-017a).
 *
 * <p>Dois pontos de entrada compartilham a mesma logica: {@link #executar(UUID)}, usado pela
 * interface web administrativa (ja autenticada via JWT, sem restricao adicional por professor —
 * FR-017a), e {@link #executar(UUID, UUID)}, usado pelo fluxo Telegram/n8n, que exige que o aluno
 * esteja associado ao {@code professorId} informado (FR-018), lancando {@link
 * AlunoNaoAssociadoException} (-> 403 {@code ALUNO_NAO_ASSOCIADO}) caso contrario.
 */
@Service
public class ConsultarHistoricoUseCase {

  private final AlunoRepository alunoRepository;
  private final AulaRepository aulaRepository;
  private final RelatorioAulaRepository relatorioAulaRepository;
  private final RelatorioSemestralRepository relatorioSemestralRepository;
  private final ProfessorAlunoRepository professorAlunoRepository;

  public ConsultarHistoricoUseCase(
      AlunoRepository alunoRepository,
      AulaRepository aulaRepository,
      RelatorioAulaRepository relatorioAulaRepository,
      RelatorioSemestralRepository relatorioSemestralRepository,
      ProfessorAlunoRepository professorAlunoRepository) {
    this.alunoRepository = alunoRepository;
    this.aulaRepository = aulaRepository;
    this.relatorioAulaRepository = relatorioAulaRepository;
    this.relatorioSemestralRepository = relatorioSemestralRepository;
    this.professorAlunoRepository = professorAlunoRepository;
  }

  /** Uso pela interface web administrativa: sem escopo por professor (FR-017a). */
  public HistoricoAlunoResponseDto executar(UUID alunoId) {
    return executar(alunoId, null);
  }

  /**
   * Uso pelo fluxo Telegram/n8n: {@code professorId} nao nulo aciona o controle de acesso FR-018.
   */
  public HistoricoAlunoResponseDto executar(UUID alunoId, UUID professorId) {
    Aluno aluno =
        alunoRepository
            .findById(alunoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno nao encontrado"));

    if (professorId != null
        && !professorAlunoRepository.existsByProfessorIdAndAlunoId(professorId, alunoId)) {
      throw new AlunoNaoAssociadoException("Aluno nao esta associado a este professor");
    }

    List<ItemHistoricoDto> itens = new ArrayList<>();

    for (RelatorioAula relatorio :
        relatorioAulaRepository.findByAlunoIdOrderByCriadoEmAsc(alunoId)) {
      if (relatorio.getStatus() != StatusRelatorioAula.APROVADO) {
        continue;
      }
      var dataAula =
          aulaRepository.findById(relatorio.getAulaId()).map(Aula::getDataAula).orElse(null);
      itens.add(
          ItemHistoricoDto.deAula(
              relatorio.getId(),
              dataAula,
              relatorio.getStatus().name(),
              relatorio.getPdfUrl(),
              relatorio.getAprovadoEm()));
    }

    for (RelatorioSemestral relatorio :
        relatorioSemestralRepository.findByAlunoIdOrderByPeriodoInicioAsc(alunoId)) {
      if (relatorio.getStatus() != StatusRelatorioSemestral.APROVADO) {
        continue;
      }
      itens.add(
          ItemHistoricoDto.deSemestral(
              relatorio.getId(),
              relatorio.getPeriodoInicio(),
              relatorio.getPeriodoFim(),
              relatorio.getStatus().name(),
              relatorio.getPdfUrl(),
              relatorio.getAprovadoEm()));
    }

    itens.sort(Comparator.comparing(ItemHistoricoDto::dataOrdenacao));

    return new HistoricoAlunoResponseDto(new AlunoResumoDto(aluno.getId(), aluno.getNome()), itens);
  }
}
