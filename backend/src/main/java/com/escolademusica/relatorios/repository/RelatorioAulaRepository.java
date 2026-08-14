package com.escolademusica.relatorios.repository;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioAula.StatusRelatorioAula;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de persistencia para {@link RelatorioAula}. A interface estende {@link JpaRepository}; o
 * Spring Data JPA gera a implementacao concreta (RelatorioAulaRepositoryImpl) em tempo de execucao,
 * dispensando uma classe de adaptador manual.
 */
public interface RelatorioAulaRepository extends JpaRepository<RelatorioAula, UUID> {

  Optional<RelatorioAula> findByAulaId(UUID aulaId);

  /** Historico cronologico por aluno (FR-017), ordenado pela data da aula associada. */
  List<RelatorioAula> findByAlunoIdOrderByCriadoEmAsc(UUID alunoId);

  List<RelatorioAula> findByProfessorIdAndAlunoId(UUID professorId, UUID alunoId);

  /** Relatorios de aula aprovados de um aluno, usados na consolidacao semestral (FR-012/FR-013). */
  List<RelatorioAula> findByAlunoIdAndStatus(UUID alunoId, StatusRelatorioAula status);

  /**
   * Rascunho/pendencia mais recente de um par professor+aluno, independentemente do canal de origem
   * (spec 002, FR-022a): alimenta a oferta de retomada antes de iniciar um novo relatorio.
   */
  Optional<RelatorioAula> findFirstByProfessorIdAndAlunoIdAndStatusInOrderByAtualizadoEmDesc(
      UUID professorId, UUID alunoId, Collection<StatusRelatorioAula> status);
}
