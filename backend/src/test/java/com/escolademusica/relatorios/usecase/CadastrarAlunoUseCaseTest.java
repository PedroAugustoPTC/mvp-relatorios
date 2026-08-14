package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.domain.exception.ConflitoException;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.service.CriptografiaService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CadastrarAlunoUseCaseTest {

  @Mock private AlunoRepository alunoRepository;
  @Mock private ProfessorAlunoRepository professorAlunoRepository;
  @Mock private ProfessorRepository professorRepository;
  @Mock private CriptografiaService criptografiaService;

  private CadastrarAlunoUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new CadastrarAlunoUseCase(
            alunoRepository, professorAlunoRepository, professorRepository, criptografiaService);
  }

  @Test
  void deveCadastrarAlunoMaiorDeIdadeSemNomeResponsavel() {
    when(criptografiaService.hash("12345678901")).thenReturn("hash123");
    when(alunoRepository.findByCpfHash("hash123")).thenReturn(Optional.empty());
    when(criptografiaService.encrypt("12345678901")).thenReturn(new byte[] {1, 2, 3});
    when(alunoRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Aluno aluno = invocation.getArgument(0);
              aluno.setId(UUID.randomUUID());
              return aluno;
            });

    Aluno aluno =
        useCase.executar(
            "Joao Adulto", LocalDate.now().minusYears(30), "12345678901", null, List.of());

    assertThat(aluno.getNome()).isEqualTo("Joao Adulto");
    assertThat(aluno.getCpfHash()).isEqualTo("hash123");
  }

  @Test
  void deveRejeitarCpfDuplicado() {
    when(criptografiaService.hash("12345678901")).thenReturn("hash123");
    when(alunoRepository.findByCpfHash("hash123")).thenReturn(Optional.of(new Aluno()));

    assertThatThrownBy(
            () ->
                useCase.executar(
                    "Joao", LocalDate.now().minusYears(30), "12345678901", null, List.of()))
        .isInstanceOf(ConflitoException.class)
        .extracting(ex -> ((ConflitoException) ex).getCodigo())
        .isEqualTo("ALUNO_CPF_DUPLICADO");

    verify(alunoRepository, never()).save(any());
  }

  @Test
  void deveExigirNomeResponsavelQuandoMenorDeIdade() {
    when(criptografiaService.hash(any())).thenReturn("hash-menor");
    when(alunoRepository.findByCpfHash("hash-menor")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                useCase.executar(
                    "Crianca", LocalDate.now().minusYears(10), "98765432100", null, List.of()))
        .isInstanceOf(IllegalArgumentException.class);

    verify(alunoRepository, never()).save(any());
  }

  @Test
  void deveAssociarProfessoresInformados() {
    UUID professorId = UUID.randomUUID();
    when(criptografiaService.hash(any())).thenReturn("hash-assoc");
    when(alunoRepository.findByCpfHash("hash-assoc")).thenReturn(Optional.empty());
    when(criptografiaService.encrypt(any())).thenReturn(new byte[] {1});
    when(professorRepository.existsById(professorId)).thenReturn(true);
    when(alunoRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Aluno aluno = invocation.getArgument(0);
              aluno.setId(UUID.randomUUID());
              return aluno;
            });

    useCase.executar(
        "Joao", LocalDate.now().minusYears(30), "11122233344", null, List.of(professorId));

    verify(professorAlunoRepository, times(1)).save(any());
  }

  @Test
  void deveExigirNomeResponsavelQuandoMenorDeIdadeENomeResponsavelEmBranco() {
    when(criptografiaService.hash(any())).thenReturn("hash-menor-branco");
    when(alunoRepository.findByCpfHash("hash-menor-branco")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                useCase.executar(
                    "Crianca", LocalDate.now().minusYears(10), "98765432100", "   ", List.of()))
        .isInstanceOf(IllegalArgumentException.class);

    verify(alunoRepository, never()).save(any());
  }

  @Test
  void deveCadastrarMenorDeIdadeComNomeResponsavelPreenchido() {
    when(criptografiaService.hash(any())).thenReturn("hash-menor-ok");
    when(alunoRepository.findByCpfHash("hash-menor-ok")).thenReturn(Optional.empty());
    when(criptografiaService.encrypt(any())).thenReturn(new byte[] {1});
    when(alunoRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Aluno aluno = invocation.getArgument(0);
              aluno.setId(UUID.randomUUID());
              return aluno;
            });

    Aluno aluno =
        useCase.executar(
            "Crianca", LocalDate.now().minusYears(10), "98765432100", "Responsavel Legal", null);

    assertThat(aluno.getNomeResponsavel()).isEqualTo("Responsavel Legal");
  }

  @Test
  void deveLancarQuandoProfessorInformadoNaoExiste() {
    UUID professorId = UUID.randomUUID();
    when(criptografiaService.hash(any())).thenReturn("hash-inexistente");
    when(alunoRepository.findByCpfHash("hash-inexistente")).thenReturn(Optional.empty());
    when(criptografiaService.encrypt(any())).thenReturn(new byte[] {1});
    when(professorRepository.existsById(professorId)).thenReturn(false);
    when(alunoRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Aluno aluno = invocation.getArgument(0);
              aluno.setId(UUID.randomUUID());
              return aluno;
            });

    assertThatThrownBy(
            () ->
                useCase.executar(
                    "Joao",
                    LocalDate.now().minusYears(30),
                    "55566677788",
                    null,
                    List.of(professorId)))
        .isInstanceOf(
            com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException.class);
  }
}
