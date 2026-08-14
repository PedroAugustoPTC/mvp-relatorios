package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.Aula;
import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioAula.StatusRelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.RelatorioSemestral.StatusRelatorioSemestral;
import com.escolademusica.relatorios.domain.exception.PeriodoSemRelatoriosException;
import com.escolademusica.relatorios.dto.RelatorioSemestralResponseDto;
import com.escolademusica.relatorios.gateway.LlmGateway;
import com.escolademusica.relatorios.mapper.RelatorioAulaMapper;
import com.escolademusica.relatorios.mapper.RelatorioSemestralMapper;
import com.escolademusica.relatorios.repository.AulaRepository;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import com.escolademusica.relatorios.service.RelatorioSemestralPdfService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GerarRelatorioSemestralUseCaseTest {

  @Mock private RelatorioSemestralRepository relatorioSemestralRepository;
  @Mock private AulaRepository aulaRepository;
  @Mock private RelatorioAulaRepository relatorioAulaRepository;
  @Mock private LlmGateway llmGateway;
  @Mock private RelatorioSemestralPdfService pdfService;

  private GerarRelatorioSemestralUseCase useCase;
  private final UUID alunoId = UUID.randomUUID();
  private final UUID professorId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    ContarRelatoriosPeriodoUseCase contarRelatoriosPeriodoUseCase =
        new ContarRelatoriosPeriodoUseCase(aulaRepository, relatorioAulaRepository);
    useCase =
        new GerarRelatorioSemestralUseCase(
            relatorioSemestralRepository,
            contarRelatoriosPeriodoUseCase,
            llmGateway,
            new RelatorioAulaMapper(objectMapper),
            new RelatorioSemestralMapper(objectMapper),
            pdfService);
  }

  @Test
  void deveConsolidarEGerarPdfQuandoHaRelatoriosAprovadosNoPeriodo() {
    UUID aulaId = UUID.randomUUID();
    Aula aula = new Aula();
    aula.setId(aulaId);
    aula.setAlunoId(alunoId);
    aula.setDataAula(LocalDate.of(2026, 3, 10));

    RelatorioAula relatorioAula = new RelatorioAula();
    relatorioAula.setId(UUID.randomUUID());
    relatorioAula.setAulaId(aulaId);
    relatorioAula.setAlunoId(alunoId);
    relatorioAula.setStatus(StatusRelatorioAula.APROVADO);
    relatorioAula.setConteudosTrabalhados("[\"Escalas\"]");
    relatorioAula.setDificuldades("[]");
    relatorioAula.setAtividadesPropostas("[]");

    when(aulaRepository.findByAlunoIdAndDataAulaBetween(
            alunoId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)))
        .thenReturn(List.of(aula));
    when(relatorioAulaRepository.findByAlunoIdAndStatus(alunoId, StatusRelatorioAula.APROVADO))
        .thenReturn(List.of(relatorioAula));
    when(llmGateway.consolidarRelatorioSemestral(any()))
        .thenReturn(
            """
            {"informacoesGerais":{"nome":"Maria"},"frequenciaEEstudo":{},"tecnica":{},\
            "musicalidade":{},"leituraEMemorizacao":{},"pontosDeAtencao":{},\
            "estrategiasPedagogicas":{},"acompanhamentoFamiliar":{},\
            "planejamentoProximoSemestre":{},"parecerFinal":"Otimo progresso"}""");
    when(relatorioSemestralRepository.save(any(RelatorioSemestral.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RelatorioSemestralResponseDto resposta = useCase.gerar(alunoId, professorId, "2026-1");

    assertThat(resposta.status()).isEqualTo(StatusRelatorioSemestral.PENDENTE_REVISAO.name());
    assertThat(resposta.quantidadeRelatoriosAulaConsiderados()).isEqualTo(1);
    assertThat(resposta.parecerFinal()).isEqualTo("Otimo progresso");
    verify(pdfService).gerarEAplicarPdf(any(RelatorioSemestral.class));
  }

  @Test
  void deveLancarPeriodoSemRelatoriosQuandoNaoHaRelatorioAprovadoNoPeriodo() {
    when(aulaRepository.findByAlunoIdAndDataAulaBetween(
            alunoId, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30)))
        .thenReturn(List.of());

    assertThatThrownBy(() -> useCase.gerar(alunoId, professorId, "2026-1"))
        .isInstanceOf(PeriodoSemRelatoriosException.class);

    verify(relatorioSemestralRepository, never()).save(any());
    verify(pdfService, never()).gerarEAplicarPdf(any());
  }
}
