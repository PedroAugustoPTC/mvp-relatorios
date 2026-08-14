package com.escolademusica.relatorios.repository;

import com.escolademusica.relatorios.domain.Aula;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de persistencia para {@link Aula}. A interface estende {@link JpaRepository}; o Spring Data
 * JPA gera a implementacao concreta (AulaRepositoryImpl) em tempo de execucao, dispensando uma
 * classe de adaptador manual.
 */
public interface AulaRepository extends JpaRepository<Aula, UUID> {

  List<Aula> findByProfessorIdAndAlunoId(UUID professorId, UUID alunoId);

  List<Aula> findByAlunoId(UUID alunoId);

  /** Aulas do aluno cuja dataAula cai dentro do periodo semestral selecionado (FR-012). */
  List<Aula> findByAlunoIdAndDataAulaBetween(UUID alunoId, LocalDate inicio, LocalDate fim);
}
