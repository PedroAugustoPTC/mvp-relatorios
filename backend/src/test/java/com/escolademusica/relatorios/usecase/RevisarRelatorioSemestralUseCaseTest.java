package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.RelatorioSemestral.StatusRelatorioSemestral;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.RelatorioSemestralResponseDto;
import com.escolademusica.relatorios.gateway.LlmGateway;
import com.escolademusica.relatorios.mapper.RelatorioSemestralMapper;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import com.escolademusica.relatorios.service.RelatorioSemestralPdfService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevisarRelatorioSemestralUseCaseTest {

  @Mock private RelatorioSemestralRepository relatorioSemestralRepository;
  @Mock private LlmGateway llmGateway;
  @Mock private RelatorioSemestralPdfService pdfService;

  private RevisarRelatorioSemestralUseCase useCase;
  private final UUID relatorioId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    RelatorioSemestralMapper mapper = new RelatorioSemestralMapper(new ObjectMapper());
    useCase =
        new RevisarRelatorioSemestralUseCase(
            relatorioSemestralRepository, llmGateway, mapper, pdfService);
  }

  @Test
  void deveIncrementarVersaoEGerarNovoPdfAoAplicarInstrucao() {
    RelatorioSemestral relatorio = new RelatorioSemestral();
    relatorio.setId(relatorioId);
    relatorio.setStatus(StatusRelatorioSemestral.PENDENTE_REVISAO);
    relatorio.setVersao(1);
    relatorio.setParecerFinal("parecer original");
    when(relatorioSemestralRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));
    when(llmGateway.revisarRelatorioSemestral(any(), any()))
        .thenReturn(
            """
            {"informacoesGerais":{},"frequenciaEEstudo":{},"tecnica":{},"musicalidade":{},\
            "leituraEMemorizacao":{},"pontosDeAtencao":{},"estrategiasPedagogicas":{},\
            "acompanhamentoFamiliar":{},"planejamentoProximoSemestre":{},\
            "parecerFinal":"parecer atualizado"}""");

    RelatorioSemestralResponseDto resposta = useCase.revisar(relatorioId, "Ajuste o parecer final");

    assertThat(relatorio.getVersao()).isEqualTo(2);
    assertThat(resposta.versao()).isEqualTo(2);
    assertThat(resposta.parecerFinal()).isEqualTo("parecer atualizado");
    assertThat(resposta.status()).isEqualTo(StatusRelatorioSemestral.PENDENTE_REVISAO.name());
    verify(pdfService).gerarEAplicarPdf(relatorio);
    verify(relatorioSemestralRepository).save(relatorio);
  }

  @Test
  void deveLancarNaoEncontradoQuandoRelatorioInexistente() {
    when(relatorioSemestralRepository.findById(relatorioId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.revisar(relatorioId, "instrucao qualquer"))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
