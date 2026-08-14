package com.escolademusica.relatorios.repository;

import com.escolademusica.relatorios.domain.Administrador;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de persistencia para {@link Administrador}. A interface estende {@link JpaRepository}; o
 * Spring Data JPA gera a implementacao concreta em tempo de execucao, dispensando uma classe de
 * adaptador manual.
 */
public interface AdministradorRepository extends JpaRepository<Administrador, UUID> {

  Optional<Administrador> findByEmail(String email);
}
