package com.escolademusica.relatorios.repository;

import com.escolademusica.relatorios.domain.RelatorioSemestral;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de persistencia para {@link RelatorioSemestral}. A interface estende {@link JpaRepository};
 * o Spring Data JPA gera a implementacao concreta (RelatorioSemestralRepositoryImpl) em tempo de
 * execucao, dispensando uma classe de adaptador manual.
 */
public interface RelatorioSemestralRepository extends JpaRepository<RelatorioSemestral, UUID> {

  /** Historico por aluno (FR-017), ordenado cronologicamente por periodo. */
  List<RelatorioSemestral> findByAlunoIdOrderByPeriodoInicioAsc(UUID alunoId);
}
