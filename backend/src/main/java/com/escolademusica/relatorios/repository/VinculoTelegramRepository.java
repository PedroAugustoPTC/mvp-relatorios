package com.escolademusica.relatorios.repository;

import com.escolademusica.relatorios.domain.VinculoTelegram;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de persistencia para {@link VinculoTelegram}. A interface estende {@link JpaRepository}; o
 * Spring Data JPA gera a implementacao concreta (VinculoTelegramRepositoryImpl) em tempo de
 * execucao, dispensando uma classe de adaptador manual.
 */
public interface VinculoTelegramRepository extends JpaRepository<VinculoTelegram, UUID> {

  Optional<VinculoTelegram> findByTelegramUserId(String telegramUserId);

  Optional<VinculoTelegram> findByProfessorId(UUID professorId);
}
