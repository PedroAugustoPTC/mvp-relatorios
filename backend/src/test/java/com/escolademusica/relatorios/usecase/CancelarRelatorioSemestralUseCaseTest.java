package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.RelatorioSemestral.StatusRelatorioSemestral;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Testes de {@link CancelarRelatorioSemestralUseCase} (T068). */
class CancelarRelatorioSemestralUseCaseTest {

  private RelatorioSemestralRepository repository;
  private CancelarRelatorioSemestralUseCase useCase;

  @BeforeEach
  void setUp() {
    repository = Mockito.mock(RelatorioSemestralRepository.class);
    useCase = new CancelarRelatorioSemestralUseCase(repository);
  }

  private RelatorioSemestral relatorioCom(StatusRelatorioSemestral status) {
    RelatorioSemestral relatorio = new RelatorioSemestral();
    relatorio.setId(UUID.randomUUID());
    relatorio.setStatus(status);
    relatorio.setAtualizadoEm(OffsetDateTime.now().minusDays(1));
    return relatorio;
  }

  @Test
  void deveCancelarRelatorioPendenteDeRevisao() {
    RelatorioSemestral relatorio = relatorioCom(StatusRelatorioSemestral.PENDENTE_REVISAO);
    OffsetDateTime antes = relatorio.getAtualizadoEm();
    when(repository.findById(relatorio.getId())).thenReturn(Optional.of(relatorio));

    useCase.cancelar(relatorio.getId());

    assertThat(relatorio.getStatus()).isEqualTo(StatusRelatorioSemestral.CANCELADO);
    assertThat(relatorio.getAtualizadoEm()).isAfter(antes);
    verify(repository).save(relatorio);
  }

  @Test
  void naoDeveCancelarRelatorioJaAprovado() {
    RelatorioSemestral relatorio = relatorioCom(StatusRelatorioSemestral.APROVADO);
    when(repository.findById(relatorio.getId())).thenReturn(Optional.of(relatorio));

    assertThatThrownBy(() -> useCase.cancelar(relatorio.getId()))
        .isInstanceOf(IllegalStateException.class);

    verify(repository, never()).save(Mockito.any());
  }

  @Test
  void naoDeveCancelarRelatorioJaCancelado() {
    RelatorioSemestral relatorio = relatorioCom(StatusRelatorioSemestral.CANCELADO);
    when(repository.findById(relatorio.getId())).thenReturn(Optional.of(relatorio));

    assertThatThrownBy(() -> useCase.cancelar(relatorio.getId()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void deveLancarQuandoRelatorioNaoExiste() {
    UUID inexistente = UUID.randomUUID();
    when(repository.findById(inexistente)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.cancelar(inexistente))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
