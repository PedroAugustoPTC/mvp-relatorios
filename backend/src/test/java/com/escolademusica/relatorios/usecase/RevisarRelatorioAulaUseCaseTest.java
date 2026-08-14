package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
class RevisarRelatorioAulaUseCaseTest {

  @Mock private RelatorioAulaRepository relatorioAulaRepository;
  @Mock private LlmGateway llmGateway;
  @Mock private RelatorioAulaPdfService pdfService;

  private RevisarRelatorioAulaUseCase useCase;
  private final UUID relatorioId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    RelatorioAulaMapper mapper = new RelatorioAulaMapper(new ObjectMapper());
    useCase =
        new RevisarRelatorioAulaUseCase(relatorioAulaRepository, llmGateway, mapper, pdfService);
  }

  @Test
  void deveIncrementarVersaoEGerarNovoPdfAoAplicarInstrucao() {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setId(relatorioId);
    relatorio.setStatus(StatusRelatorioAula.PENDENTE_REVISAO);
    relatorio.setVersao(1);
    relatorio.setConteudosTrabalhados("[\"Ritmo\"]");
    relatorio.setDificuldades("[\"Ritmo na segunda parte\"]");
    relatorio.setAtividadesPropostas("[]");
    when(relatorioAulaRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));
    when(llmGateway.revisarRelatorioAula(any(), any()))
        .thenReturn(
            """
            {"conteudosTrabalhados":["Ritmo"],"evolucao":"boa",\
            "dificuldades":["Dificuldade ajustada"],"atividadesPropostas":[],\
            "observacoes":"","perguntasPendentes":[]}""");

    EstruturarRelatorioResponseDto resposta =
        useCase.revisar(relatorioId, "Mude a dificuldade para ritmo");

    assertThat(relatorio.getVersao()).isEqualTo(2);
    assertThat(resposta.versao()).isEqualTo(2);
    assertThat(resposta.dificuldades()).containsExactly("Dificuldade ajustada");
    assertThat(resposta.status()).isEqualTo(StatusRelatorioAula.PENDENTE_REVISAO.name());
    verify(pdfService).gerarEAplicarPdf(relatorio);
    verify(relatorioAulaRepository).save(relatorio);
  }

  @Test
  void deveLancarNaoEncontradoQuandoRelatorioInexistente() {
    when(relatorioAulaRepository.findById(relatorioId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.revisar(relatorioId, "instrucao qualquer"))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
