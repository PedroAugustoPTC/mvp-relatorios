package com.escolademusica.relatorios.repository;

import com.escolademusica.relatorios.domain.Professor;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de persistencia para {@link Professor}. A interface estende {@link JpaRepository}; o Spring
 * Data JPA gera a implementacao concreta (ProfessorRepositoryImpl) em tempo de execucao,
 * dispensando uma classe de adaptador manual.
 */
public interface ProfessorRepository extends JpaRepository<Professor, UUID> {

  Optional<Professor> findByEmail(String email);

  Optional<Professor> findByCodigoVinculacao(String codigoVinculacao);
}
