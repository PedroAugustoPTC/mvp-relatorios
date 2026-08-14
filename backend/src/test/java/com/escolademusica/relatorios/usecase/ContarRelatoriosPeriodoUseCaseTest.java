package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.Aula;
import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioAula.StatusRelatorioAula;
import com.escolademusica.relatorios.dto.ContagemRelatoriosResponseDto;
import com.escolademusica.relatorios.repository.AulaRepository;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContarRelatoriosPeriodoUseCaseTest {

  @Mock private AulaRepository aulaRepository;
  @Mock private RelatorioAulaRepository relatorioAulaRepository;

  private ContarRelatoriosPeriodoUseCase useCase;
  private final UUID alunoId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    useCase = new ContarRelatoriosPeriodoUseCase(aulaRepository, relatorioAulaRepository);
  }

  private Aula aula(UUID id, LocalDate data) {
    Aula aula = new Aula();
    aula.setId(id);
    aula.setAlunoId(alunoId);
    aula.setDataAula(data);
    return aula;
  }

  private RelatorioAula relatorio(UUID aulaId, StatusRelatorioAula status) {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setId(UUID.randomUUID());
    relatorio.setAulaId(aulaId);
    relatorio.setAlunoId(alunoId);
    relatorio.setStatus(status);
    return relatorio;
  }

  @Test
  void deveContarApenasRelatoriosAprovadosDeAulasDentroDoPeriodo() {
    UUID aula1 = UUID.randomUUID();
    UUID aula2 = UUID.randomUUID();
    when(aulaRepository.findByAlunoIdAndDataAulaBetween(
            alunoId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)))
        .thenReturn(
            List.of(aula(aula1, LocalDate.of(2026, 2, 10)), aula(aula2, LocalDate.of(2026, 3, 5))));
    when(relatorioAulaRepository.findByAlunoIdAndStatus(alunoId, StatusRelatorioAula.APROVADO))
        .thenReturn(
            List.of(
                relatorio(aula1, StatusRelatorioAula.APROVADO),
                relatorio(aula2, StatusRelatorioAula.APROVADO)));

    ContagemRelatoriosResponseDto resposta = useCase.contar(alunoId, "2026-1");

    assertThat(resposta.quantidadeRelatoriosAula()).isEqualTo(2);
  }

  @Test
  void deveRetornarZeroQuandoNaoHaAulasNoPeriodo() {
    when(aulaRepository.findByAlunoIdAndDataAulaBetween(
            alunoId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)))
        .thenReturn(List.of());

    ContagemRelatoriosResponseDto resposta = useCase.contar(alunoId, "2026-1");

    assertThat(resposta.quantidadeRelatoriosAula()).isZero();
  }

  @Test
  void naoDeveContarRelatoriosPendentesDeRevisao() {
    UUID aula1 = UUID.randomUUID();
    when(aulaRepository.findByAlunoIdAndDataAulaBetween(
            alunoId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)))
        .thenReturn(List.of(aula(aula1, LocalDate.of(2026, 2, 10))));
    when(relatorioAulaRepository.findByAlunoIdAndStatus(alunoId, StatusRelatorioAula.APROVADO))
        .thenReturn(List.of());

    ContagemRelatoriosResponseDto resposta = useCase.contar(alunoId, "2026-1");

    assertThat(resposta.quantidadeRelatoriosAula()).isZero();
  }
}
