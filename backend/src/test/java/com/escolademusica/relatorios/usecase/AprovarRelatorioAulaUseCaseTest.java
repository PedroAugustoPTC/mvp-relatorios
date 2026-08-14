package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioAula.StatusRelatorioAula;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.AprovarRelatorioResponseDto;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AprovarRelatorioAulaUseCaseTest {

  @Mock private RelatorioAulaRepository relatorioAulaRepository;

  private AprovarRelatorioAulaUseCase useCase;
  private final UUID relatorioId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    useCase = new AprovarRelatorioAulaUseCase(relatorioAulaRepository);
  }

  @Test
  void deveAprovarQuandoVersaoConfirmadaBateComVersaoVigente() {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setId(relatorioId);
    relatorio.setStatus(StatusRelatorioAula.PENDENTE_REVISAO);
    relatorio.setVersao(2);
    when(relatorioAulaRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));

    AprovarRelatorioResponseDto resposta = useCase.aprovar(relatorioId, 2);

    assertThat(resposta.status()).isEqualTo(StatusRelatorioAula.APROVADO.name());
    assertThat(resposta.aprovadoEm()).isNotNull();
    assertThat(relatorio.getStatus()).isEqualTo(StatusRelatorioAula.APROVADO);
  }

  @Test
  void deveRejeitarComConflitoQuandoVersaoConfirmadaEstaDesatualizada() {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setId(relatorioId);
    relatorio.setStatus(StatusRelatorioAula.PENDENTE_REVISAO);
    relatorio.setVersao(3);
    when(relatorioAulaRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));

    assertThatThrownBy(() -> useCase.aprovar(relatorioId, 2))
        .isInstanceOf(IllegalStateException.class);
    assertThat(relatorio.getStatus()).isEqualTo(StatusRelatorioAula.PENDENTE_REVISAO);
  }

  @Test
  void deveRejeitarComConflitoQuandoJaAprovado() {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setId(relatorioId);
    relatorio.setStatus(StatusRelatorioAula.APROVADO);
    relatorio.setVersao(1);
    when(relatorioAulaRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));

    assertThatThrownBy(() -> useCase.aprovar(relatorioId, 1))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void deveLancarNaoEncontradoQuandoRelatorioInexistente() {
    when(relatorioAulaRepository.findById(relatorioId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.aprovar(relatorioId, 1))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
