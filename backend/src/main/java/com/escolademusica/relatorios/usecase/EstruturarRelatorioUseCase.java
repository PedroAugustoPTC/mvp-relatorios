package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.RelatorioAulaLlmPayloadDto;
import com.escolademusica.relatorios.gateway.LlmGateway;
import com.escolademusica.relatorios.mapper.RelatorioAulaMapper;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.service.RelatorioAulaPdfService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estrutura a transcricao de uma aula em um relatorio (T042): chama o {@link LlmGateway}, valida o
 * JSON retornado e detecta perguntas de acompanhamento pendentes (FR-007). Quando nao ha pendencia,
 * o relatorio avanca para PENDENTE_REVISAO e o PDF de conferencia e gerado automaticamente.
 */
@Service
public class EstruturarRelatorioUseCase {

  private final RelatorioAulaRepository relatorioAulaRepository;
  private final LlmGateway llmGateway;
  private final RelatorioAulaMapper mapper;
  private final RelatorioAulaPdfService pdfService;

  public EstruturarRelatorioUseCase(
      RelatorioAulaRepository relatorioAulaRepository,
      LlmGateway llmGateway,
      RelatorioAulaMapper mapper,
      RelatorioAulaPdfService pdfService) {
    this.relatorioAulaRepository = relatorioAulaRepository;
    this.llmGateway = llmGateway;
    this.mapper = mapper;
    this.pdfService = pdfService;
  }

  @Transactional
  public EstruturarRelatorioResponseDto estruturar(UUID aulaId) {
    RelatorioAula relatorio =
        relatorioAulaRepository
            .findByAulaId(aulaId)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Relatorio de aula nao encontrado para aulaId=" + aulaId));

    String respostaLlm = llmGateway.estruturarRelatorioAula(relatorio.getTranscricao());
    RelatorioAulaLlmPayloadDto payload = mapper.lerPayloadLlm(respostaLlm);
    mapper.aplicarPayload(relatorio, payload);
    relatorio.setAtualizadoEm(OffsetDateTime.now());

    List<String> perguntasPendentes = payload.perguntasPendentes();
    if (perguntasPendentes.isEmpty()) {
      relatorio.marcarPendenteRevisao();
      pdfService.gerarEAplicarPdf(relatorio);
    }

    relatorioAulaRepository.save(relatorio);
    return mapper.paraResponseDto(relatorio, perguntasPendentes);
  }
}
