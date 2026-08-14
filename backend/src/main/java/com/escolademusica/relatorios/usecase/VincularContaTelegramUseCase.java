package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.VinculoTelegram;
import com.escolademusica.relatorios.domain.exception.ConflitoException;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.repository.VinculoTelegramRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valida o codigo de vinculacao informado pelo professor via Telegram, cria o {@link
 * VinculoTelegram} e invalida o codigo apos o uso (uso unico, FR-002/FR-003).
 */
@Service
public class VincularContaTelegramUseCase {

  private final ProfessorRepository professorRepository;
  private final VinculoTelegramRepository vinculoTelegramRepository;

  public VincularContaTelegramUseCase(
      ProfessorRepository professorRepository,
      VinculoTelegramRepository vinculoTelegramRepository) {
    this.professorRepository = professorRepository;
    this.vinculoTelegramRepository = vinculoTelegramRepository;
  }

  /** Resultado da vinculacao, conforme contracts/api-n8n-integration.md. */
  public record ResultadoVinculacao(java.util.UUID professorId, String nomeProfessor) {}

  @Transactional
  public ResultadoVinculacao executar(String telegramUserId, String codigoVinculacao) {
    OffsetDateTime agora = OffsetDateTime.now();

    Professor professor =
        professorRepository
            .findByCodigoVinculacao(codigoVinculacao)
            .filter(p -> p.codigoVinculacaoValidoEm(agora))
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException("Codigo de vinculacao invalido ou expirado"));

    Optional<VinculoTelegram> vinculoExistentePorTelegram =
        vinculoTelegramRepository.findByTelegramUserId(telegramUserId);
    if (vinculoExistentePorTelegram.isPresent()
        && !vinculoExistentePorTelegram.get().getProfessorId().equals(professor.getId())) {
      throw new ConflitoException(
          "TELEGRAM_JA_VINCULADO", "Esta conta do Telegram ja esta vinculada a outro professor.");
    }

    Optional<VinculoTelegram> vinculoExistentePorProfessor =
        vinculoTelegramRepository.findByProfessorId(professor.getId());
    if (vinculoExistentePorProfessor.isPresent()
        && !vinculoExistentePorProfessor.get().getTelegramUserId().equals(telegramUserId)) {
      throw new ConflitoException(
          "PROFESSOR_JA_VINCULADO", "Este professor ja possui uma conta do Telegram vinculada.");
    }

    if (vinculoExistentePorTelegram.isEmpty()) {
      VinculoTelegram vinculo = new VinculoTelegram();
      vinculo.setProfessorId(professor.getId());
      vinculo.setTelegramUserId(telegramUserId);
      vinculo.setVinculadoEm(agora);
      vinculoTelegramRepository.save(vinculo);
    }

    // Uso unico: invalida o codigo apos a vinculacao bem-sucedida (FR-002).
    professor.setCodigoVinculacao(null);
    professor.setCodigoVinculacaoExpiraEm(null);
    professor.setAtualizadoEm(agora);
    professorRepository.save(professor);

    return new ResultadoVinculacao(professor.getId(), professor.getNome());
  }
}
