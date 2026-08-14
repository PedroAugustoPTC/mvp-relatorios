package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioAula.StatusRelatorioAula;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Testes de {@link CancelarRelatorioAulaUseCase} (T037). */
class CancelarRelatorioAulaUseCaseTest {

  private RelatorioAulaRepository relatorioAulaRepository;
  private CancelarRelatorioAulaUseCase useCase;

  @BeforeEach
  void setUp() {
    relatorioAulaRepository = Mockito.mock(RelatorioAulaRepository.class);
    useCase = new CancelarRelatorioAulaUseCase(relatorioAulaRepository);
  }

  private RelatorioAula relatorioCom(StatusRelatorioAula status) {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setId(UUID.randomUUID());
    relatorio.setStatus(status);
    relatorio.setAtualizadoEm(OffsetDateTime.now().minusDays(1));
    return relatorio;
  }

  @Test
  void deveCancelarRascunhoRegistrandoStatusEDataDeAtualizacao() {
    RelatorioAula relatorio = relatorioCom(StatusRelatorioAula.RASCUNHO);
    OffsetDateTime antes = relatorio.getAtualizadoEm();
    when(relatorioAulaRepository.findById(relatorio.getId())).thenReturn(Optional.of(relatorio));

    useCase.cancelar(relatorio.getId());

    ArgumentCaptor<RelatorioAula> captor = ArgumentCaptor.forClass(RelatorioAula.class);
    verify(relatorioAulaRepository).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(StatusRelatorioAula.CANCELADO);
    assertThat(captor.getValue().getAtualizadoEm()).isAfter(antes);
  }

  @Test
  void deveCancelarRelatorioPendenteDeRevisao() {
    RelatorioAula relatorio = relatorioCom(StatusRelatorioAula.PENDENTE_REVISAO);
    when(relatorioAulaRepository.findById(relatorio.getId())).thenReturn(Optional.of(relatorio));

    useCase.cancelar(relatorio.getId());

    assertThat(relatorio.getStatus()).isEqualTo(StatusRelatorioAula.CANCELADO);
  }

  @Test
  void naoDeveCancelarRelatorioJaAprovado() {
    RelatorioAula relatorio = relatorioCom(StatusRelatorioAula.APROVADO);
    when(relatorioAulaRepository.findById(relatorio.getId())).thenReturn(Optional.of(relatorio));

    assertThatThrownBy(() -> useCase.cancelar(relatorio.getId()))
        .isInstanceOf(IllegalStateException.class);

    verify(relatorioAulaRepository, never()).save(Mockito.any());
  }

  @Test
  void naoDeveCancelarRelatorioJaCancelado() {
    RelatorioAula relatorio = relatorioCom(StatusRelatorioAula.CANCELADO);
    when(relatorioAulaRepository.findById(relatorio.getId())).thenReturn(Optional.of(relatorio));

    assertThatThrownBy(() -> useCase.cancelar(relatorio.getId()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void deveLancarQuandoRelatorioNaoExiste() {
    UUID inexistente = UUID.randomUUID();
    when(relatorioAulaRepository.findById(inexistente)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.cancelar(inexistente))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
