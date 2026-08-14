package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.SemestreDisponivelDto;
import com.escolademusica.relatorios.repository.AlunoRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListarSemestresDisponiveisUseCaseTest {

  @Mock private AlunoRepository alunoRepository;

  private ListarSemestresDisponiveisUseCase useCase;
  private final UUID alunoId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    useCase = new ListarSemestresDisponiveisUseCase(alunoRepository);
  }

  @Test
  void deveListarTodosOsSemestresDoCadastroAteOAtual() {
    Aluno aluno = new Aluno();
    aluno.setId(alunoId);
    aluno.setCriadoEm(OffsetDateTime.of(2025, 8, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    when(alunoRepository.findById(alunoId)).thenReturn(Optional.of(aluno));

    List<SemestreDisponivelDto> semestres = useCase.listar(alunoId);

    assertThat(semestres).isNotEmpty();
    assertThat(semestres.get(0).chave()).isEqualTo("2025-2");
    assertThat(semestres.get(0).inicio()).isEqualTo(LocalDate.of(2025, 7, 1));
    assertThat(semestres.get(0).fim()).isEqualTo(LocalDate.of(2025, 12, 31));
    assertThat(semestres).allSatisfy(s -> assertThat(s.rotulo()).isNotBlank());
    // A lista deve estar em ordem cronologica crescente.
    assertThat(semestres).isSortedAccordingTo((a, b) -> a.inicio().compareTo(b.inicio()));
  }

  @Test
  void deveUsarDataAtualQuandoCriadoEmNulo() {
    Aluno aluno = new Aluno();
    aluno.setId(alunoId);
    aluno.setCriadoEm(null);
    when(alunoRepository.findById(alunoId)).thenReturn(Optional.of(aluno));

    List<SemestreDisponivelDto> semestres = useCase.listar(alunoId);

    assertThat(semestres).isNotEmpty();
  }

  @Test
  void deveConsiderarPrimeiroSemestreQuandoCadastroNoInicioDoAno() {
    Aluno aluno = new Aluno();
    aluno.setId(alunoId);
    aluno.setCriadoEm(OffsetDateTime.of(2024, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    when(alunoRepository.findById(alunoId)).thenReturn(Optional.of(aluno));

    List<SemestreDisponivelDto> semestres = useCase.listar(alunoId);

    assertThat(semestres.get(0).chave()).isEqualTo("2024-1");
  }

  @Test
  void deveLancarNaoEncontradoQuandoAlunoInexistente() {
    when(alunoRepository.findById(alunoId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.listar(alunoId))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
