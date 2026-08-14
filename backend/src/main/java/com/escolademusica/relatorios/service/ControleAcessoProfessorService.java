package com.escolademusica.relatorios.service;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.exception.AlunoNaoAssociadoException;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Controle de acesso dos endpoints do portal do professor (spec 002, FR-018 aplicado ao canal web).
 *
 * <p>O JWT prova QUEM e o professor, nunca A QUE dados ele tem direito: todo endpoint que recebe um
 * {@code alunoId} ou {@code relatorioId} vindo da URL precisa confirmar o vinculo antes de
 * responder. Centralizar essa checagem aqui evita que um endpoint novo esqueca de faze-la.
 *
 * <p>Dois codigos de resposta distintos, deliberadamente:
 *
 * <ul>
 *   <li><b>403</b> para aluno nao associado — o professor sabe que o aluno existe (ele veio de uma
 *       lista ou de um link), so nao e dele.
 *   <li><b>404</b> para relatorio de outro professor — responder 403 confirmaria a existencia
 *       daquele relatorio a quem nao deveria sequer saber disso.
 * </ul>
 */
@Service
public class ControleAcessoProfessorService {

  private final ProfessorAlunoRepository professorAlunoRepository;
  private final RelatorioAulaRepository relatorioAulaRepository;
  private final RelatorioSemestralRepository relatorioSemestralRepository;

  public ControleAcessoProfessorService(
      ProfessorAlunoRepository professorAlunoRepository,
      RelatorioAulaRepository relatorioAulaRepository,
      RelatorioSemestralRepository relatorioSemestralRepository) {
    this.professorAlunoRepository = professorAlunoRepository;
    this.relatorioAulaRepository = relatorioAulaRepository;
    this.relatorioSemestralRepository = relatorioSemestralRepository;
  }

  /**
   * @throws AlunoNaoAssociadoException (-> 403) se o aluno nao for do professor autenticado
   */
  public void exigirAlunoDoProfessor(UUID professorId, UUID alunoId) {
    if (!professorAlunoRepository.existsByProfessorIdAndAlunoId(professorId, alunoId)) {
      throw new AlunoNaoAssociadoException("Aluno nao esta associado a este professor");
    }
  }

  /**
   * @throws RecursoNaoEncontradoException (-> 404) se o relatorio nao existir ou nao for dele
   */
  public RelatorioAula exigirRelatorioAulaDoProfessor(UUID professorId, UUID relatorioId) {
    return relatorioAulaRepository
        .findById(relatorioId)
        .filter(relatorio -> relatorio.getProfessorId().equals(professorId))
        .orElseThrow(
            () ->
                new RecursoNaoEncontradoException(
                    "Relatorio de aula nao encontrado: " + relatorioId));
  }

  /**
   * @throws RecursoNaoEncontradoException (-> 404) se o relatorio nao existir ou nao for dele
   */
  public RelatorioSemestral exigirRelatorioSemestralDoProfessor(
      UUID professorId, UUID relatorioId) {
    return relatorioSemestralRepository
        .findById(relatorioId)
        .filter(relatorio -> relatorio.getProfessorId().equals(professorId))
        .orElseThrow(
            () ->
                new RecursoNaoEncontradoException(
                    "Relatorio semestral nao encontrado: " + relatorioId));
  }
}
