package com.escolademusica.relatorios.usecase;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.exception.PeriodoSemRelatoriosException;
import com.escolademusica.relatorios.dto.RelatorioSemestralLlmPayloadDto;
import com.escolademusica.relatorios.dto.RelatorioSemestralResponseDto;
import com.escolademusica.relatorios.gateway.LlmGateway;
import com.escolademusica.relatorios.mapper.RelatorioAulaMapper;
import com.escolademusica.relatorios.mapper.RelatorioSemestralMapper;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import com.escolademusica.relatorios.service.RelatorioSemestralPdfService;
import com.escolademusica.relatorios.usecase.support.PeriodoSemestralResolver;
import com.escolademusica.relatorios.usecase.support.PeriodoSemestralResolver.Periodo;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera o relatorio semestral consolidado (T074, FR-013/FR-013a): exige ao menos um relatorio de
 * aula aprovado no periodo (FR-014), consolida via {@link LlmGateway} e gera o PDF de conferencia.
 */
@Service
public class GerarRelatorioSemestralUseCase {

  private final RelatorioSemestralRepository relatorioSemestralRepository;
  private final ContarRelatoriosPeriodoUseCase contarRelatoriosPeriodoUseCase;
  private final LlmGateway llmGateway;
  private final RelatorioAulaMapper relatorioAulaMapper;
  private final RelatorioSemestralMapper relatorioSemestralMapper;
  private final RelatorioSemestralPdfService pdfService;

  public GerarRelatorioSemestralUseCase(
      RelatorioSemestralRepository relatorioSemestralRepository,
      ContarRelatoriosPeriodoUseCase contarRelatoriosPeriodoUseCase,
      LlmGateway llmGateway,
      RelatorioAulaMapper relatorioAulaMapper,
      RelatorioSemestralMapper relatorioSemestralMapper,
      RelatorioSemestralPdfService pdfService) {
    this.relatorioSemestralRepository = relatorioSemestralRepository;
    this.contarRelatoriosPeriodoUseCase = contarRelatoriosPeriodoUseCase;
    this.llmGateway = llmGateway;
    this.relatorioAulaMapper = relatorioAulaMapper;
    this.relatorioSemestralMapper = relatorioSemestralMapper;
    this.pdfService = pdfService;
  }

  @Transactional
  public RelatorioSemestralResponseDto gerar(UUID alunoId, UUID professorId, String periodoChave) {
    Periodo periodo = PeriodoSemestralResolver.resolverPorChave(periodoChave);

    List<RelatorioAula> relatoriosAula =
        contarRelatoriosPeriodoUseCase.listarAprovadosNoPeriodo(
            alunoId, periodo.inicio(), periodo.fim());
    if (relatoriosAula.isEmpty()) {
      // FR-014: nao gera relatorio semestral (nem seu PDF) quando nao ha relatorio de aula no
      // periodo. Defesa em profundidade: a contagem ja e exibida ao professor antes deste
      // endpoint ser chamado (T073), mas o backend nunca confia apenas no cliente.
      throw new PeriodoSemRelatoriosException(
          "Nenhum relatorio de aula aprovado encontrado no periodo " + periodoChave);
    }

    List<String> relatoriosAulaJson =
        relatoriosAula.stream()
            .map(r -> relatorioAulaMapper.paraPayload(r, List.of()))
            .map(relatorioAulaMapper::escreverJsonPayload)
            .toList();

    String respostaLlm = llmGateway.consolidarRelatorioSemestral(relatoriosAulaJson);
    RelatorioSemestralLlmPayloadDto payload = relatorioSemestralMapper.lerPayloadLlm(respostaLlm);

    OffsetDateTime agora = OffsetDateTime.now();
    RelatorioSemestral relatorio = new RelatorioSemestral();
    relatorio.setAlunoId(alunoId);
    relatorio.setProfessorId(professorId);
    relatorio.setPeriodoInicio(periodo.inicio());
    relatorio.setPeriodoFim(periodo.fim());
    relatorio.setQuantidadeRelatoriosAulaConsiderados(relatoriosAula.size());
    relatorio.setVersao(1);
    relatorio.setStatus(RelatorioSemestral.StatusRelatorioSemestral.PENDENTE_REVISAO);
    relatorio.setCriadoEm(agora);
    relatorio.setAtualizadoEm(agora);
    relatorioSemestralMapper.aplicarPayload(relatorio, payload);

    // Persiste primeiro para obter o id gerado (usado para nomear o arquivo do PDF).
    relatorio = relatorioSemestralRepository.save(relatorio);
    pdfService.gerarEAplicarPdf(relatorio);
    relatorio = relatorioSemestralRepository.save(relatorio);

    return relatorioSemestralMapper.paraResponseDto(relatorio);
  }
}
