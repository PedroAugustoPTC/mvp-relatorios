package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.CanalOrigem;
import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioAula.StatusRelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.RelatorioSemestral.StatusRelatorioSemestral;
import com.escolademusica.relatorios.dto.RascunhoPendenteResponseDto;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * Testes de {@link DetectarRascunhoPendenteUseCase} (T037, FR-022a): a deteccao precisa enxergar
 * rascunhos de QUALQUER canal, informar de onde vieram e ignorar relatorios ja encerrados.
 */
class DetectarRascunhoPendenteUseCaseTest {

  private RelatorioAulaRepository relatorioAulaRepository;
  private RelatorioSemestralRepository relatorioSemestralRepository;
  private DetectarRascunhoPendenteUseCase useCase;

  private final UUID professorId = UUID.randomUUID();
  private final UUID alunoId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    relatorioAulaRepository = Mockito.mock(RelatorioAulaRepository.class);
    relatorioSemestralRepository = Mockito.mock(RelatorioSemestralRepository.class);
    useCase =
        new DetectarRascunhoPendenteUseCase(relatorioAulaRepository, relatorioSemestralRepository);

    when(relatorioAulaRepository.findFirstByProfessorIdAndAlunoIdAndStatusInOrderByAtualizadoEmDesc(
            any(), any(), any()))
        .thenReturn(Optional.empty());
    when(relatorioSemestralRepository
            .findFirstByProfessorIdAndAlunoIdAndStatusInOrderByAtualizadoEmDesc(
                any(), any(), any()))
        .thenReturn(Optional.empty());
  }

  @Test
  void deveDetectarRascunhoDeAulaIniciadoNoTelegramQuandoConsultadoPeloPortal() {
    OffsetDateTime atualizadoEm = OffsetDateTime.now().minusHours(2);
    RelatorioAula relatorio = new RelatorioAula();
    UUID relatorioId = UUID.randomUUID();
    relatorio.setId(relatorioId);
    relatorio.setStatus(StatusRelatorioAula.PENDENTE_REVISAO);
    relatorio.setCanalOrigem(CanalOrigem.TELEGRAM);
    relatorio.setAtualizadoEm(atualizadoEm);
    when(relatorioAulaRepository.findFirstByProfessorIdAndAlunoIdAndStatusInOrderByAtualizadoEmDesc(
            eq(professorId), eq(alunoId), any()))
        .thenReturn(Optional.of(relatorio));

    RascunhoPendenteResponseDto resultado = useCase.detectar(professorId, alunoId);

    assertThat(resultado.existeRascunho()).isTrue();
    assertThat(resultado.tipo()).isEqualTo("AULA");
    assertThat(resultado.relatorioId()).isEqualTo(relatorioId);
    assertThat(resultado.status()).isEqualTo("PENDENTE_REVISAO");
    assertThat(resultado.canalOrigem()).isEqualTo("TELEGRAM");
    assertThat(resultado.atualizadoEm()).isEqualTo(atualizadoEm);
  }

  @Test
  void deveConsultarApenasStatusEmAbertoIgnorandoAprovadosECancelados() {
    useCase.detectar(professorId, alunoId);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<StatusRelatorioAula>> captor =
        ArgumentCaptor.forClass(Collection.class);
    verify(relatorioAulaRepository)
        .findFirstByProfessorIdAndAlunoIdAndStatusInOrderByAtualizadoEmDesc(
            eq(professorId), eq(alunoId), captor.capture());

    assertThat(captor.getValue())
        .containsExactlyInAnyOrder(
            StatusRelatorioAula.RASCUNHO, StatusRelatorioAula.PENDENTE_REVISAO);
  }

  @Test
  void deveDetectarPendenciaSemestralQuandoNaoHaRascunhoDeAula() {
    RelatorioSemestral semestral = new RelatorioSemestral();
    UUID relatorioId = UUID.randomUUID();
    semestral.setId(relatorioId);
    semestral.setStatus(StatusRelatorioSemestral.PENDENTE_REVISAO);
    semestral.setCanalOrigem(CanalOrigem.WEB);
    semestral.setAtualizadoEm(OffsetDateTime.now());
    when(relatorioSemestralRepository
            .findFirstByProfessorIdAndAlunoIdAndStatusInOrderByAtualizadoEmDesc(
                eq(professorId),
                eq(alunoId),
                eq(List.of(StatusRelatorioSemestral.PENDENTE_REVISAO))))
        .thenReturn(Optional.of(semestral));

    RascunhoPendenteResponseDto resultado = useCase.detectar(professorId, alunoId);

    assertThat(resultado.existeRascunho()).isTrue();
    assertThat(resultado.tipo()).isEqualTo("SEMESTRAL");
    assertThat(resultado.relatorioId()).isEqualTo(relatorioId);
    assertThat(resultado.canalOrigem()).isEqualTo("WEB");
  }

  @Test
  void rascunhoDeAulaDevePrevalecerSobrePendenciaSemestralSemSequerConsultaLaASemestral() {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setId(UUID.randomUUID());
    relatorio.setStatus(StatusRelatorioAula.RASCUNHO);
    relatorio.setCanalOrigem(CanalOrigem.WEB);
    relatorio.setAtualizadoEm(OffsetDateTime.now());
    when(relatorioAulaRepository.findFirstByProfessorIdAndAlunoIdAndStatusInOrderByAtualizadoEmDesc(
            any(), any(), any()))
        .thenReturn(Optional.of(relatorio));

    RascunhoPendenteResponseDto resultado = useCase.detectar(professorId, alunoId);

    assertThat(resultado.tipo()).isEqualTo("AULA");
    verify(relatorioSemestralRepository, never())
        .findFirstByProfessorIdAndAlunoIdAndStatusInOrderByAtualizadoEmDesc(any(), any(), any());
  }

  @Test
  void deveResponderVazioQuandoNaoHaNenhumaPendencia() {
    RascunhoPendenteResponseDto resultado = useCase.detectar(professorId, alunoId);

    assertThat(resultado.existeRascunho()).isFalse();
    assertThat(resultado.tipo()).isNull();
    assertThat(resultado.relatorioId()).isNull();
    assertThat(resultado.status()).isNull();
    assertThat(resultado.canalOrigem()).isNull();
    assertThat(resultado.atualizadoEm()).isNull();
  }
}
