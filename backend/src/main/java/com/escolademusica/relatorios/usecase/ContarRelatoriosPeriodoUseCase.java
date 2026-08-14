package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.Aula;
import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioAula.StatusRelatorioAula;
import com.escolademusica.relatorios.dto.ContagemRelatoriosResponseDto;
import com.escolademusica.relatorios.repository.AulaRepository;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.usecase.support.PeriodoSemestralResolver;
import com.escolademusica.relatorios.usecase.support.PeriodoSemestralResolver.Periodo;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Conta quantos relatorios de aula aprovados existem no periodo selecionado, antes de gerar o
 * relatorio semestral (T073, FR-012). Somente relatorios com status {@code APROVADO} contam para a
 * consolidacao, pois relatorios ainda pendentes de revisao podem mudar de conteudo.
 */
@Service
public class ContarRelatoriosPeriodoUseCase {

  private final AulaRepository aulaRepository;
  private final RelatorioAulaRepository relatorioAulaRepository;

  public ContarRelatoriosPeriodoUseCase(
      AulaRepository aulaRepository, RelatorioAulaRepository relatorioAulaRepository) {
    this.aulaRepository = aulaRepository;
    this.relatorioAulaRepository = relatorioAulaRepository;
  }

  public ContagemRelatoriosResponseDto contar(UUID alunoId, String periodoChave) {
    Periodo periodo = PeriodoSemestralResolver.resolverPorChave(periodoChave);
    List<RelatorioAula> relatorios =
        listarAprovadosNoPeriodo(alunoId, periodo.inicio(), periodo.fim());
    return new ContagemRelatoriosResponseDto(relatorios.size());
  }

  /**
   * Lista os relatorios de aula aprovados de um aluno cuja {@code Aula} associada cai dentro do
   * periodo informado. Reaproveitado por {@code GerarRelatorioSemestralUseCase} para buscar o
   * conteudo a consolidar sem duplicar a logica de interseccao aula/relatorio.
   */
  public List<RelatorioAula> listarAprovadosNoPeriodo(
      UUID alunoId, LocalDate inicio, LocalDate fim) {
    Set<UUID> aulaIdsNoPeriodo =
        aulaRepository.findByAlunoIdAndDataAulaBetween(alunoId, inicio, fim).stream()
            .map(Aula::getId)
            .collect(Collectors.toSet());
    if (aulaIdsNoPeriodo.isEmpty()) {
      return List.of();
    }
    return relatorioAulaRepository
        .findByAlunoIdAndStatus(alunoId, StatusRelatorioAula.APROVADO)
        .stream()
        .filter(r -> aulaIdsNoPeriodo.contains(r.getAulaId()))
        .collect(Collectors.toList());
  }
}
