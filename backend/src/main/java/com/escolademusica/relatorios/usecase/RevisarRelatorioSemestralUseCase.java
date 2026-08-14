package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.RelatorioSemestralLlmPayloadDto;
import com.escolademusica.relatorios.dto.RelatorioSemestralResponseDto;
import com.escolademusica.relatorios.gateway.LlmGateway;
import com.escolademusica.relatorios.mapper.RelatorioSemestralMapper;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import com.escolademusica.relatorios.service.RelatorioSemestralPdfService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica uma solicitacao de alteracao em linguagem natural sobre um relatorio semestral ja
 * consolidado (T075, FR-015): reenvia o relatorio atual + a instrucao ao {@link LlmGateway},
 * incrementa a versao e gera um novo PDF. O relatorio permanece (ou volta a) PENDENTE_REVISAO,
 * exigindo nova confirmacao explicita antes de ser aprovado.
 */
@Service
public class RevisarRelatorioSemestralUseCase {

  private final RelatorioSemestralRepository relatorioSemestralRepository;
  private final LlmGateway llmGateway;
  private final RelatorioSemestralMapper mapper;
  private final RelatorioSemestralPdfService pdfService;

  public RevisarRelatorioSemestralUseCase(
      RelatorioSemestralRepository relatorioSemestralRepository,
      LlmGateway llmGateway,
      RelatorioSemestralMapper mapper,
      RelatorioSemestralPdfService pdfService) {
    this.relatorioSemestralRepository = relatorioSemestralRepository;
    this.llmGateway = llmGateway;
    this.mapper = mapper;
    this.pdfService = pdfService;
  }

  @Transactional
  public RelatorioSemestralResponseDto revisar(UUID relatorioId, String instrucao) {
    RelatorioSemestral relatorio =
        relatorioSemestralRepository
            .findById(relatorioId)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Relatorio semestral nao encontrado: " + relatorioId));

    RelatorioSemestralLlmPayloadDto payloadAtual = mapper.paraPayload(relatorio);
    String jsonAtual = mapper.escreverJsonPayload(payloadAtual);

    String respostaLlm = llmGateway.revisarRelatorioSemestral(jsonAtual, instrucao);
    RelatorioSemestralLlmPayloadDto novoPayload = mapper.lerPayloadLlm(respostaLlm);
    mapper.aplicarPayload(relatorio, novoPayload);

    relatorio.solicitarAlteracao();
    relatorio.setStatus(RelatorioSemestral.StatusRelatorioSemestral.PENDENTE_REVISAO);
    relatorio.setAtualizadoEm(OffsetDateTime.now());
    pdfService.gerarEAplicarPdf(relatorio);

    relatorioSemestralRepository.save(relatorio);
    return mapper.paraResponseDto(relatorio);
  }
}
