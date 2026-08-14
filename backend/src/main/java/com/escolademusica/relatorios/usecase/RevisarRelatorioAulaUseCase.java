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
 * Aplica uma solicitacao de alteracao em linguagem natural sobre um relatorio de aula ja
 * estruturado (T044, FR-009): reenvia o relatorio atual + a instrucao ao {@link LlmGateway},
 * incrementa a versao e gera um novo PDF.
 */
@Service
public class RevisarRelatorioAulaUseCase {

  private final RelatorioAulaRepository relatorioAulaRepository;
  private final LlmGateway llmGateway;
  private final RelatorioAulaMapper mapper;
  private final RelatorioAulaPdfService pdfService;

  public RevisarRelatorioAulaUseCase(
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
  public EstruturarRelatorioResponseDto revisar(UUID relatorioId, String instrucao) {
    RelatorioAula relatorio =
        relatorioAulaRepository
            .findById(relatorioId)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Relatorio de aula nao encontrado: " + relatorioId));

    RelatorioAulaLlmPayloadDto payloadAtual = mapper.paraPayload(relatorio, List.of());
    String jsonAtual = mapper.escreverJsonPayload(payloadAtual);

    String respostaLlm = llmGateway.revisarRelatorioAula(jsonAtual, instrucao);
    RelatorioAulaLlmPayloadDto novoPayload = mapper.lerPayloadLlm(respostaLlm);
    mapper.aplicarPayload(relatorio, novoPayload);

    relatorio.solicitarAlteracao();
    relatorio.marcarPendenteRevisao();
    relatorio.setAtualizadoEm(OffsetDateTime.now());
    pdfService.gerarEAplicarPdf(relatorio);

    relatorioAulaRepository.save(relatorio);
    return mapper.paraResponseDto(relatorio, novoPayload.perguntasPendentes());
  }
}
