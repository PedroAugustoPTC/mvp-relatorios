package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioAula.StatusRelatorioAula;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
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
class ResponderPerguntaUseCaseTest {

  @Mock private RelatorioAulaRepository relatorioAulaRepository;
  @Mock private RelatorioAulaPdfService pdfService;

  private ResponderPerguntaUseCase useCase;
  private final UUID relatorioId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    RelatorioAulaMapper mapper = new RelatorioAulaMapper(new ObjectMapper());
    useCase = new ResponderPerguntaUseCase(relatorioAulaRepository, mapper, pdfService);
  }

  @Test
  void deveAnexarRespostaEGerarPdf() {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setId(relatorioId);
    relatorio.setObservacoes("Observacao original");
    relatorio.setStatus(StatusRelatorioAula.RASCUNHO);
    when(relatorioAulaRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));

    EstruturarRelatorioResponseDto resposta =
        useCase.responder(relatorioId, "Sim, praticar o exercicio de ritmo.");

    assertThat(resposta.status()).isEqualTo(StatusRelatorioAula.PENDENTE_REVISAO.name());
    assertThat(resposta.perguntasPendentes()).isEmpty();
    assertThat(relatorio.getObservacoes())
        .contains("Observacao original")
        .contains("Sim, praticar o exercicio de ritmo.");
    verify(pdfService).gerarEAplicarPdf(relatorio);
    verify(relatorioAulaRepository).save(relatorio);
  }

  @Test
  void deveUsarRespostaComoUnicaObservacaoQuandoObservacoesAtuaisEstaoEmBranco() {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setId(relatorioId);
    relatorio.setObservacoes("   ");
    when(relatorioAulaRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));

    useCase.responder(relatorioId, "Primeira observacao");

    assertThat(relatorio.getObservacoes())
        .isEqualTo("Pergunta de acompanhamento respondida pelo professor: Primeira observacao");
  }

  @Test
  void deveLancarNaoEncontradoQuandoRelatorioInexistente() {
    when(relatorioAulaRepository.findById(relatorioId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.responder(relatorioId, "resposta qualquer"))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
