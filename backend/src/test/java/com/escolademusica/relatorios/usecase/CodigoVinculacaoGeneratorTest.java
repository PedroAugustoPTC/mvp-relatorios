package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CodigoVinculacaoGeneratorTest {

  @Mock private ProfessorRepository professorRepository;

  private CodigoVinculacaoGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new CodigoVinculacaoGenerator(professorRepository);
  }

  @Test
  void deveGerarCodigoDeOitoCaracteresQuandoUnico() {
    when(professorRepository.findByCodigoVinculacao(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(Optional.empty());

    String codigo = generator.gerarCodigoUnico();

    assertThat(codigo).hasSize(8);
    assertThat(codigo).matches("[A-HJ-NP-Z2-9]{8}");
  }

  @Test
  void deveTentarNovamenteQuandoCodigoJaExiste() {
    Professor existente = new Professor();
    when(professorRepository.findByCodigoVinculacao(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(Optional.of(existente))
        .thenReturn(Optional.empty());

    String codigo = generator.gerarCodigoUnico();

    assertThat(codigo).hasSize(8);
  }

  @Test
  void deveLancarQuandoNaoConseguirGerarCodigoUnicoAposTentativas() {
    Professor existente = new Professor();
    when(professorRepository.findByCodigoVinculacao(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(Optional.of(existente));

    assertThatThrownBy(() -> generator.gerarCodigoUnico())
        .isInstanceOf(IllegalStateException.class);
  }
}
