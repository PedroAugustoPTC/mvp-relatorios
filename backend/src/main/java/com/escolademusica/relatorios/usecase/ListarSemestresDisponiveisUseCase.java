package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.SemestreDisponivelDto;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.usecase.support.PeriodoSemestralResolver;
import com.escolademusica.relatorios.usecase.support.PeriodoSemestralResolver.Periodo;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Gera o menu pre-definido de semestres selecionaveis para um aluno (T072, FR-011): do semestre em
 * que o aluno foi cadastrado ate o semestre corrente (inclusive), em vez de exigir que o professor
 * digite datas livremente.
 */
@Service
public class ListarSemestresDisponiveisUseCase {

  private final AlunoRepository alunoRepository;

  public ListarSemestresDisponiveisUseCase(AlunoRepository alunoRepository) {
    this.alunoRepository = alunoRepository;
  }

  public List<SemestreDisponivelDto> listar(UUID alunoId) {
    Aluno aluno =
        alunoRepository
            .findById(alunoId)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException("Aluno nao encontrado: " + alunoId));

    LocalDate inicioCadastro =
        aluno.getCriadoEm() != null ? aluno.getCriadoEm().toLocalDate() : LocalDate.now();
    LocalDate hoje = LocalDate.now();

    int anoInicial = inicioCadastro.getYear();
    int semestreInicial = inicioCadastro.getMonthValue() <= 6 ? 1 : 2;
    int anoFinal = hoje.getYear();
    int semestreFinal = hoje.getMonthValue() <= 6 ? 1 : 2;

    List<SemestreDisponivelDto> semestres = new ArrayList<>();
    int ano = anoInicial;
    int semestre = semestreInicial;
    while (ano < anoFinal || (ano == anoFinal && semestre <= semestreFinal)) {
      Periodo periodo = PeriodoSemestralResolver.resolver(ano, semestre);
      semestres.add(
          new SemestreDisponivelDto(
              periodo.chave(), periodo.rotulo(), periodo.inicio(), periodo.fim()));
      if (semestre == 1) {
        semestre = 2;
      } else {
        semestre = 1;
        ano += 1;
      }
    }
    return semestres;
  }
}
