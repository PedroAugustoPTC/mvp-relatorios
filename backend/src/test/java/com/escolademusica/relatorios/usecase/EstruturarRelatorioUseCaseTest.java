package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioAula.StatusRelatorioAula;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
import com.escolademusica.relatorios.gateway.LlmGateway;
import com.escolademusica.relatorios.mapper.RelatorioAulaMapper;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.service.RelatorioAulaPdfService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EstruturarRelatorioUseCaseTest {

  @Mock private RelatorioAulaRepository relatorioAulaRepository;
  @Mock private LlmGateway llmGateway;
  @Mock private RelatorioAulaPdfService pdfService;

  private RelatorioAulaMapper mapper;
  private EstruturarRelatorioUseCase useCase;

  private final UUID aulaId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    mapper = new RelatorioAulaMapper(new ObjectMapper());
    useCase =
        new EstruturarRelatorioUseCase(relatorioAulaRepository, llmGateway, mapper, pdfService);
  }

  private RelatorioAula relatorioComTranscricao() {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setId(UUID.randomUUID());
    relatorio.setAulaId(aulaId);
    relatorio.setTranscricao("Hoje trabalhamos escalas");
    relatorio.setVersao(1);
    return relatorio;
  }

  @Test
  void deveGerarPdfEMarcarPendenteRevisaoQuandoNaoHaPerguntasPendentes() {
    RelatorioAula relatorio = relatorioComTranscricao();
    when(relatorioAulaRepository.findByAulaId(aulaId)).thenReturn(Optional.of(relatorio));
    when(llmGateway.estruturarRelatorioAula(any()))
        .thenReturn(
            """
            {"conteudosTrabalhados":["Escalas"],"evolucao":"boa","dificuldades":[],\
            "atividadesPropostas":[],"observacoes":"","perguntasPendentes":[]}""");

    EstruturarRelatorioResponseDto resposta = useCase.estruturar(aulaId);

    assertThat(resposta.status()).isEqualTo(StatusRelatorioAula.PENDENTE_REVISAO.name());
    assertThat(resposta.perguntasPendentes()).isEmpty();
    verify(pdfService).gerarEAplicarPdf(relatorio);
    verify(relatorioAulaRepository).save(relatorio);
  }

  @Test
  void deveManterRascunhoSemGerarPdfQuandoHaPerguntasPendentes() {
    RelatorioAula relatorio = relatorioComTranscricao();
    when(relatorioAulaRepository.findByAulaId(aulaId)).thenReturn(Optional.of(relatorio));
    when(llmGateway.estruturarRelatorioAula(any()))
        .thenReturn(
            """
            {"conteudosTrabalhados":["Escalas"],"evolucao":"boa","dificuldades":[],\
            "atividadesPropostas":[],"observacoes":"",\
            "perguntasPendentes":["Deseja registrar tarefa de casa?"]}""");

    EstruturarRelatorioResponseDto resposta = useCase.estruturar(aulaId);

    assertThat(resposta.status()).isEqualTo(StatusRelatorioAula.RASCUNHO.name());
    assertThat(resposta.perguntasPendentes()).containsExactly("Deseja registrar tarefa de casa?");
    verify(pdfService, never()).gerarEAplicarPdf(any());
  }

  @Test
  void deveLancarNaoEncontradoQuandoAulaNaoTemRelatorio() {
    when(relatorioAulaRepository.findByAulaId(aulaId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.estruturar(aulaId))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
