package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
import com.escolademusica.relatorios.mapper.RelatorioAulaMapper;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.service.RelatorioAulaPdfService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica a resposta do professor a uma pergunta de acompanhamento pendente (T043, FR-007).
 *
 * <p><b>Simplificacao deliberada para o MVP:</b> o dominio nao persiste a lista individual de
 * perguntas pendentes (elas sao derivadas a cada chamada ao LlmGateway, nao um estado
 * duravel/encadeado). Por isso, este use case adota a abordagem minima que atende a intencao da
 * FR-007 — nao bloquear a entrega do relatorio por falta de um detalhe: a resposta do professor e
 * anexada as observacoes do relatorio e, apos qualquer resposta fornecida, o relatorio e tratado
 * como completo (sem mais pendencias), avancando para PENDENTE_REVISAO e gerando o PDF. Um
 * mecanismo de multiplas perguntas encadeadas por relatorio fica fora do escopo do MVP.
 */
@Service
public class ResponderPerguntaUseCase {

  private final RelatorioAulaRepository relatorioAulaRepository;
  private final RelatorioAulaMapper mapper;
  private final RelatorioAulaPdfService pdfService;

  public ResponderPerguntaUseCase(
      RelatorioAulaRepository relatorioAulaRepository,
      RelatorioAulaMapper mapper,
      RelatorioAulaPdfService pdfService) {
    this.relatorioAulaRepository = relatorioAulaRepository;
    this.mapper = mapper;
    this.pdfService = pdfService;
  }

  @Transactional
  public EstruturarRelatorioResponseDto responder(UUID relatorioId, String resposta) {
    RelatorioAula relatorio =
        relatorioAulaRepository
            .findById(relatorioId)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Relatorio de aula nao encontrado: " + relatorioId));

    String observacoesAtuais = relatorio.getObservacoes();
    String novaLinha = "Pergunta de acompanhamento respondida pelo professor: " + resposta;
    relatorio.setObservacoes(
        (observacoesAtuais == null || observacoesAtuais.isBlank())
            ? novaLinha
            : observacoesAtuais + "\n" + novaLinha);

    relatorio.marcarPendenteRevisao();
    relatorio.setAtualizadoEm(OffsetDateTime.now());
    pdfService.gerarEAplicarPdf(relatorio);

    relatorioAulaRepository.save(relatorio);
    return mapper.paraResponseDto(relatorio, List.of());
  }
}
