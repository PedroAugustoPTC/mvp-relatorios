package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReemitirCodigoVinculacaoUseCaseTest {

  @Mock private ProfessorRepository professorRepository;

  private ReemitirCodigoVinculacaoUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new ReemitirCodigoVinculacaoUseCase(
            professorRepository, new CodigoVinculacaoGenerator(professorRepository));
  }

  @Test
  void deveGerarNovoCodigoParaProfessorExistente() {
    UUID professorId = UUID.randomUUID();
    Professor professor = new Professor();
    professor.setId(professorId);
    professor.setCodigoVinculacao("EXPIRADO1");
    professor.setCodigoVinculacaoExpiraEm(OffsetDateTime.now().minusDays(1));

    when(professorRepository.findById(professorId)).thenReturn(Optional.of(professor));
    when(professorRepository.findByCodigoVinculacao(any())).thenReturn(Optional.empty());
    when(professorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Professor atualizado = useCase.executar(professorId);

    assertThat(atualizado.getCodigoVinculacao()).isNotEqualTo("EXPIRADO1");
    assertThat(atualizado.getCodigoVinculacaoExpiraEm()).isAfter(OffsetDateTime.now());
  }

  @Test
  void deveLancarNaoEncontradoQuandoProfessorNaoExiste() {
    UUID professorId = UUID.randomUUID();
    when(professorRepository.findById(professorId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executar(professorId))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
