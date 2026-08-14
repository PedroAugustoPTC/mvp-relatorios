package com.escolademusica.relatorios.repository;

import com.escolademusica.relatorios.domain.ProfessorAluno;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de persistencia para {@link ProfessorAluno}. A interface estende {@link JpaRepository}; o
 * Spring Data JPA gera a implementacao concreta (ProfessorAlunoRepositoryImpl) em tempo de
 * execucao, dispensando uma classe de adaptador manual.
 */
public interface ProfessorAlunoRepository extends JpaRepository<ProfessorAluno, UUID> {

  /** Usado para controle de acesso (FR-018): confirma se o aluno esta vinculado ao professor. */
  boolean existsByProfessorIdAndAlunoId(UUID professorId, UUID alunoId);

  /** Lista de selecao do bot (FR-004, US2 cenario 3). */
  List<ProfessorAluno> findByProfessorId(UUID professorId);
}
