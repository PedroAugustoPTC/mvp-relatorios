package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.domain.ProfessorAluno;
import com.escolademusica.relatorios.domain.exception.ConflitoException;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.service.CriptografiaService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cadastra um aluno, validando unicidade de CPF via hash (FR-001a), exigindo nome do responsavel
 * quando menor de idade, e associando um ou mais professores (US2 cenario 3).
 */
@Service
public class CadastrarAlunoUseCase {

  private final AlunoRepository alunoRepository;
  private final ProfessorAlunoRepository professorAlunoRepository;
  private final ProfessorRepository professorRepository;
  private final CriptografiaService criptografiaService;

  public CadastrarAlunoUseCase(
      AlunoRepository alunoRepository,
      ProfessorAlunoRepository professorAlunoRepository,
      ProfessorRepository professorRepository,
      CriptografiaService criptografiaService) {
    this.alunoRepository = alunoRepository;
    this.professorAlunoRepository = professorAlunoRepository;
    this.professorRepository = professorRepository;
    this.criptografiaService = criptografiaService;
  }

  @Transactional
  public Aluno executar(
      String nome,
      LocalDate dataNascimento,
      String cpf,
      String nomeResponsavel,
      List<UUID> professorIds) {
    String cpfHash = criptografiaService.hash(cpf);
    if (alunoRepository.findByCpfHash(cpfHash).isPresent()) {
      throw new ConflitoException(
          "ALUNO_CPF_DUPLICADO", "Ja existe um aluno cadastrado com este CPF.");
    }

    boolean menorDeIdade = new PeriodoReferencia(dataNascimento).isMenorDeIdadeHoje();
    if (menorDeIdade && (nomeResponsavel == null || nomeResponsavel.isBlank())) {
      throw new IllegalArgumentException(
          "nomeResponsavel e obrigatorio quando dataNascimento indica menor de idade");
    }

    OffsetDateTime agora = OffsetDateTime.now();

    Aluno aluno = new Aluno();
    aluno.setNome(nome);
    aluno.setDataNascimento(dataNascimento);
    aluno.setCpfCriptografado(criptografiaService.encrypt(cpf));
    aluno.setCpfHash(cpfHash);
    aluno.setNomeResponsavel(nomeResponsavel);
    aluno.setAtivo(true);
    aluno.setCriadoEm(agora);
    aluno.setAtualizadoEm(agora);

    Aluno alunoSalvo = alunoRepository.save(aluno);

    if (professorIds != null) {
      for (UUID professorId : professorIds) {
        if (!professorRepository.existsById(professorId)) {
          throw new RecursoNaoEncontradoException("Professor nao encontrado: " + professorId);
        }
        professorAlunoRepository.save(new ProfessorAluno(professorId, alunoSalvo.getId()));
      }
    }

    return alunoSalvo;
  }

  /** Encapsula a checagem de maioridade usando o helper de dominio de {@link Aluno}. */
  private static final class PeriodoReferencia {
    private final Aluno alunoTemporario;

    private PeriodoReferencia(LocalDate dataNascimento) {
      this.alunoTemporario = new Aluno();
      this.alunoTemporario.setDataNascimento(dataNascimento);
    }

    private boolean isMenorDeIdadeHoje() {
      return alunoTemporario.isMenorDeIdade();
    }
  }
}
