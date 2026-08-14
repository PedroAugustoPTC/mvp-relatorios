package com.escolademusica.relatorios.repository;

import com.escolademusica.relatorios.domain.Aluno;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de persistencia para {@link Aluno}. A interface estende {@link JpaRepository}; o Spring
 * Data JPA gera a implementacao concreta (AlunoRepositoryImpl) em tempo de execucao, dispensando
 * uma classe de adaptador manual.
 */
public interface AlunoRepository extends JpaRepository<Aluno, UUID> {

  /** Usado para checagem de duplicidade de CPF sem descriptografar (FR-001a). */
  Optional<Aluno> findByCpfHash(String cpfHash);
}
