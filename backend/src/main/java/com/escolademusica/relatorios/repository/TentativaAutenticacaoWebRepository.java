package com.escolademusica.relatorios.repository;

import com.escolademusica.relatorios.domain.TentativaAutenticacaoWeb;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de persistencia para {@link TentativaAutenticacaoWeb} (FR-004a). A interface estende {@link
 * JpaRepository}; o Spring Data JPA gera a implementacao concreta
 * (TentativaAutenticacaoWebRepositoryImpl) em tempo de execucao, dispensando uma classe de
 * adaptador manual — mesmo padrao adotado pelas demais portas de persistencia da spec 001.
 */
public interface TentativaAutenticacaoWebRepository
    extends JpaRepository<TentativaAutenticacaoWeb, UUID> {

  Optional<TentativaAutenticacaoWeb> findByIdentificador(String identificador);
}
