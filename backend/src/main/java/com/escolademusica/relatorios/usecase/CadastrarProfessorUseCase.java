package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.exception.ConflitoException;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastra um professor e gera seu codigo de vinculacao Telegram inicial, com expiracao (FR-001,
 * FR-002).
 */
@Service
public class CadastrarProfessorUseCase {

  private final ProfessorRepository professorRepository;
  private final CodigoVinculacaoGenerator codigoVinculacaoGenerator;

  public CadastrarProfessorUseCase(
      ProfessorRepository professorRepository,
      CodigoVinculacaoGenerator codigoVinculacaoGenerator) {
    this.professorRepository = professorRepository;
    this.codigoVinculacaoGenerator = codigoVinculacaoGenerator;
  }

  @Transactional
  public Professor executar(String nome, String email) {
    if (professorRepository.findByEmail(email).isPresent()) {
      throw new ConflitoException(
          "PROFESSOR_EMAIL_DUPLICADO", "Ja existe um professor cadastrado com este e-mail.");
    }

    OffsetDateTime agora = OffsetDateTime.now();

    Professor professor = new Professor();
    professor.setNome(nome);
    professor.setEmail(email);
    professor.setAtivo(true);
    professor.setCodigoVinculacao(codigoVinculacaoGenerator.gerarCodigoUnico());
    professor.setCodigoVinculacaoExpiraEm(agora.plus(CodigoVinculacaoGenerator.VALIDADE));
    professor.setCriadoEm(agora);
    professor.setAtualizadoEm(agora);

    return professorRepository.save(professor);
  }
}
