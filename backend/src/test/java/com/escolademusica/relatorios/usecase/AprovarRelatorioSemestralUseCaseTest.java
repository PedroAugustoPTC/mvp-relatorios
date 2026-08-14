package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.RelatorioSemestral.StatusRelatorioSemestral;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.AprovarRelatorioResponseDto;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AprovarRelatorioSemestralUseCaseTest {

  @Mock private RelatorioSemestralRepository relatorioSemestralRepository;

  private AprovarRelatorioSemestralUseCase useCase;
  private final UUID relatorioId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    useCase = new AprovarRelatorioSemestralUseCase(relatorioSemestralRepository);
  }

  @Test
  void deveAprovarQuandoVersaoConfirmadaBateComVersaoVigente() {
    RelatorioSemestral relatorio = new RelatorioSemestral();
    relatorio.setId(relatorioId);
    relatorio.setStatus(StatusRelatorioSemestral.PENDENTE_REVISAO);
    relatorio.setVersao(2);
    when(relatorioSemestralRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));

    AprovarRelatorioResponseDto resposta = useCase.aprovar(relatorioId, 2);

    assertThat(resposta.status()).isEqualTo(StatusRelatorioSemestral.APROVADO.name());
    assertThat(resposta.aprovadoEm()).isNotNull();
    assertThat(relatorio.getStatus()).isEqualTo(StatusRelatorioSemestral.APROVADO);
  }

  @Test
  void deveRejeitarComConflitoQuandoVersaoConfirmadaEstaDesatualizada() {
    RelatorioSemestral relatorio = new RelatorioSemestral();
    relatorio.setId(relatorioId);
    relatorio.setStatus(StatusRelatorioSemestral.PENDENTE_REVISAO);
    relatorio.setVersao(3);
    when(relatorioSemestralRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));

    assertThatThrownBy(() -> useCase.aprovar(relatorioId, 2))
        .isInstanceOf(IllegalStateException.class);
    assertThat(relatorio.getStatus()).isEqualTo(StatusRelatorioSemestral.PENDENTE_REVISAO);
  }

  @Test
  void deveRejeitarComConflitoQuandoJaAprovado() {
    RelatorioSemestral relatorio = new RelatorioSemestral();
    relatorio.setId(relatorioId);
    relatorio.setStatus(StatusRelatorioSemestral.APROVADO);
    relatorio.setVersao(1);
    when(relatorioSemestralRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));

    assertThatThrownBy(() -> useCase.aprovar(relatorioId, 1))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void deveLancarNaoEncontradoQuandoRelatorioInexistente() {
    when(relatorioSemestralRepository.findById(relatorioId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.aprovar(relatorioId, 1))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
