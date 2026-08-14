package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.exception.ConflitoException;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CadastrarProfessorUseCaseTest {

  @Mock private ProfessorRepository professorRepository;

  private CadastrarProfessorUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new CadastrarProfessorUseCase(
            professorRepository, new CodigoVinculacaoGenerator(professorRepository));
  }

  @Test
  void deveCadastrarProfessorComCodigoVinculacaoEExpiracao() {
    when(professorRepository.findByEmail("maria@escola.com")).thenReturn(Optional.empty());
    when(professorRepository.findByCodigoVinculacao(any())).thenReturn(Optional.empty());
    when(professorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Professor professor = useCase.executar("Maria Silva", "maria@escola.com");

    assertThat(professor.getNome()).isEqualTo("Maria Silva");
    assertThat(professor.getEmail()).isEqualTo("maria@escola.com");
    assertThat(professor.getCodigoVinculacao()).isNotBlank();
    assertThat(professor.getCodigoVinculacaoExpiraEm()).isNotNull();
    assertThat(professor.isAtivo()).isTrue();
  }

  @Test
  void deveRejeitarEmailJaCadastrado() {
    Professor existente = new Professor();
    existente.setEmail("maria@escola.com");
    when(professorRepository.findByEmail("maria@escola.com")).thenReturn(Optional.of(existente));

    assertThatThrownBy(() -> useCase.executar("Maria Silva", "maria@escola.com"))
        .isInstanceOf(ConflitoException.class)
        .extracting(ex -> ((ConflitoException) ex).getCodigo())
        .isEqualTo("PROFESSOR_EMAIL_DUPLICADO");
  }
}
