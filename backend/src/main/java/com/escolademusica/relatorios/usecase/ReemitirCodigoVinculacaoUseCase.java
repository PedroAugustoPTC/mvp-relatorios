package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reemite o codigo de vinculacao Telegram de um professor ja cadastrado (ex.: codigo expirado antes
 * de ser usado), FR-002.
 */
@Service
public class ReemitirCodigoVinculacaoUseCase {

  private final ProfessorRepository professorRepository;
  private final CodigoVinculacaoGenerator codigoVinculacaoGenerator;

  public ReemitirCodigoVinculacaoUseCase(
      ProfessorRepository professorRepository,
      CodigoVinculacaoGenerator codigoVinculacaoGenerator) {
    this.professorRepository = professorRepository;
    this.codigoVinculacaoGenerator = codigoVinculacaoGenerator;
  }

  @Transactional
  public Professor executar(UUID professorId) {
    Professor professor =
        professorRepository
            .findById(professorId)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException("Professor nao encontrado: " + professorId));

    OffsetDateTime agora = OffsetDateTime.now();
    professor.setCodigoVinculacao(codigoVinculacaoGenerator.gerarCodigoUnico());
    professor.setCodigoVinculacaoExpiraEm(agora.plus(CodigoVinculacaoGenerator.VALIDADE));
    professor.setAtualizadoEm(agora);

    return professorRepository.save(professor);
  }
}
