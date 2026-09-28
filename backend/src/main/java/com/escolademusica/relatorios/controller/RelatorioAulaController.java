package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.AprovarRelatorioRequestDto;
import com.escolademusica.relatorios.dto.AprovarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.ResponderPerguntaRequestDto;
import com.escolademusica.relatorios.dto.RevisarRelatorioRequestDto;
import com.escolademusica.relatorios.dto.TranscreverResponseDto;
import com.escolademusica.relatorios.mapper.RelatorioAulaMapper;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.service.PdfGeracaoService;
import com.escolademusica.relatorios.usecase.AprovarRelatorioAulaUseCase;
import com.escolademusica.relatorios.usecase.CancelarRelatorioAulaUseCase;
import com.escolademusica.relatorios.usecase.EstruturarRelatorioUseCase;
import com.escolademusica.relatorios.usecase.RegistrarAulaUseCase;
import com.escolademusica.relatorios.usecase.ResponderPerguntaUseCase;
import com.escolademusica.relatorios.usecase.RevisarRelatorioAulaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints internos (chamados pelo n8n) do fluxo de registro de aula por audio (T047, US1):
 * transcrever -> estruturar -> responder-pergunta (opcional) -> revisar (opcional) -> aprovar.
 * Protegidos pela cadeia {@code /internal/v1/**} de {@code SecurityConfig} (header {@code
 * X-N8N-Service-Token}).
 */
@RestController
@Tag(
    name = "n8n - Registro de aula",
    description = "Fluxo transcrever -> estruturar -> responder-pergunta -> revisar -> aprovar")
public class RelatorioAulaController {

  private final RegistrarAulaUseCase registrarAulaUseCase;
  private final EstruturarRelatorioUseCase estruturarRelatorioUseCase;
  private final ResponderPerguntaUseCase responderPerguntaUseCase;
  private final RevisarRelatorioAulaUseCase revisarRelatorioAulaUseCase;
  private final AprovarRelatorioAulaUseCase aprovarRelatorioAulaUseCase;
  private final CancelarRelatorioAulaUseCase cancelarRelatorioAulaUseCase;
  private final RelatorioAulaRepository relatorioAulaRepository;
  private final PdfGeracaoService pdfGeracaoService;
  private final RelatorioAulaMapper mapper;

  public RelatorioAulaController(
      RegistrarAulaUseCase registrarAulaUseCase,
      EstruturarRelatorioUseCase estruturarRelatorioUseCase,
      ResponderPerguntaUseCase responderPerguntaUseCase,
      RevisarRelatorioAulaUseCase revisarRelatorioAulaUseCase,
      AprovarRelatorioAulaUseCase aprovarRelatorioAulaUseCase,
      CancelarRelatorioAulaUseCase cancelarRelatorioAulaUseCase,
      RelatorioAulaRepository relatorioAulaRepository,
      PdfGeracaoService pdfGeracaoService,
      RelatorioAulaMapper mapper) {
    this.registrarAulaUseCase = registrarAulaUseCase;
    this.estruturarRelatorioUseCase = estruturarRelatorioUseCase;
    this.responderPerguntaUseCase = responderPerguntaUseCase;
    this.revisarRelatorioAulaUseCase = revisarRelatorioAulaUseCase;
    this.aprovarRelatorioAulaUseCase = aprovarRelatorioAulaUseCase;
    this.cancelarRelatorioAulaUseCase = cancelarRelatorioAulaUseCase;
    this.relatorioAulaRepository = relatorioAulaRepository;
    this.pdfGeracaoService = pdfGeracaoService;
    this.mapper = mapper;
  }

  /** {@code POST /internal/v1/relatorios-aula/transcrever} (multipart). */
  @Operation(summary = "Transcreve o audio de uma aula recebido via Telegram (FR-005)")
  @PostMapping(
      value = "/internal/v1/relatorios-aula/transcrever",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public TranscreverResponseDto transcrever(
      @RequestParam UUID alunoId,
      @RequestParam UUID professorId,
      @RequestParam LocalDate dataAula,
      @RequestParam MultipartFile audio) {
    byte[] audioBytes;
    try {
      audioBytes = audio.getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("Falha ao ler o arquivo de audio enviado", e);
    }
    String formato = extrairFormato(audio);

    RegistrarAulaUseCase.Resultado resultado =
        registrarAulaUseCase.registrar(professorId, alunoId, dataAula, audioBytes, formato);
    return new TranscreverResponseDto(resultado.aulaId(), resultado.transcricao());
  }

  /** {@code POST /internal/v1/relatorios-aula/{aulaId}/estruturar} */
  @Operation(
      summary =
          "Estrutura a transcricao em relatorio, retornando perguntas pendentes se houver (FR-006/FR-007)")
  @PostMapping("/internal/v1/relatorios-aula/{aulaId}/estruturar")
  public EstruturarRelatorioResponseDto estruturar(@PathVariable UUID aulaId) {
    return estruturarRelatorioUseCase.estruturar(aulaId);
  }

  /** {@code POST /internal/v1/relatorios-aula/{relatorioId}/responder-pergunta} */
  @Operation(summary = "Envia a resposta do professor a uma pergunta de acompanhamento")
  @PostMapping("/internal/v1/relatorios-aula/{relatorioId}/responder-pergunta")
  public EstruturarRelatorioResponseDto responderPergunta(
      @PathVariable UUID relatorioId, @Valid @RequestBody ResponderPerguntaRequestDto corpo) {
    return responderPerguntaUseCase.responder(relatorioId, corpo.resposta());
  }

  /** {@code POST /internal/v1/relatorios-aula/{relatorioId}/revisar} */
  @Operation(summary = "Aplica uma instrucao de alteracao em linguagem natural (FR-009)")
  @PostMapping("/internal/v1/relatorios-aula/{relatorioId}/revisar")
  public EstruturarRelatorioResponseDto revisar(
      @PathVariable UUID relatorioId, @Valid @RequestBody RevisarRelatorioRequestDto corpo) {
    return revisarRelatorioAulaUseCase.revisar(relatorioId, corpo.instrucao());
  }

  /** {@code POST /internal/v1/relatorios-aula/{relatorioId}/aprovar} */
  @Operation(
      summary = "Confirma a aprovacao explicita do professor sobre o PDF vigente (FR-008a/FR-010)")
  @PostMapping("/internal/v1/relatorios-aula/{relatorioId}/aprovar")
  public AprovarRelatorioResponseDto aprovar(
      @PathVariable UUID relatorioId, @Valid @RequestBody AprovarRelatorioRequestDto corpo) {
    return aprovarRelatorioAulaUseCase.aprovar(relatorioId, corpo.versaoConfirmada());
  }

  /**
   * {@code POST /internal/v1/relatorios-aula/{relatorioId}/cancelar} — o professor pediu pra
   * cancelar o rascunho pelo Telegram (comando "cancelar"). Mesmo {@link
   * CancelarRelatorioAulaUseCase} do canal web ({@code ProfessorPortalRelatorioAulaController}) —
   * so o canal de entrada muda.
   */
  @Operation(summary = "Cancela o rascunho de relatorio de aula (uso interno do bot do Telegram)")
  @PostMapping("/internal/v1/relatorios-aula/{relatorioId}/cancelar")
  public void cancelar(@PathVariable UUID relatorioId) {
    cancelarRelatorioAulaUseCase.cancelar(relatorioId);
  }

  /**
   * {@code GET /internal/v1/relatorios-aula/{relatorioId}} — estado atual do relatorio (campos
   * estruturados, status, perguntas pendentes, pdfUrl, versao). Usado pelo bot do Telegram quando
   * ha um rascunho em aberto para o professor (descoberto via {@code
   * /internal/v1/telegram/{telegramUserId}/rascunho-pendente}) e o n8n precisa saber exatamente o
   * que perguntar/mostrar de novo, ja que cada mensagem recebida e uma execucao isolada sem memoria
   * da anterior.
   */
  @Operation(summary = "Estado atual do relatorio de aula (uso interno do bot do Telegram)")
  @GetMapping("/internal/v1/relatorios-aula/{relatorioId}")
  public EstruturarRelatorioResponseDto consultar(@PathVariable UUID relatorioId) {
    RelatorioAula relatorio =
        relatorioAulaRepository
            .findById(relatorioId)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Relatorio de aula nao encontrado: " + relatorioId));
    return mapper.paraResponseDto(relatorio, mapper.perguntasPendentesDe(relatorio));
  }

  /**
   * {@code GET /internal/v1/relatorios-aula/{relatorioId}/pdf} — devolve os bytes do PDF vigente
   * (versao atual) para o node Telegram do n8n encaminhar ao professor. O canal web usa {@code
   * /api/v1/arquivos/**} (protegido por JWT); o n8n so possui o token de servico, daí este endpoint
   * dedicado na cadeia {@code /internal/v1/**}.
   */
  @Operation(summary = "Baixa o PDF vigente do relatorio de aula, para envio via Telegram")
  @GetMapping("/internal/v1/relatorios-aula/{relatorioId}/pdf")
  public ResponseEntity<byte[]> baixarPdf(@PathVariable UUID relatorioId) {
    Optional<RelatorioAula> relatorio = relatorioAulaRepository.findById(relatorioId);
    if (relatorio.isEmpty() || relatorio.get().getPdfUrl() == null) {
      return ResponseEntity.notFound().build();
    }
    String pdfUrl = relatorio.get().getPdfUrl();
    String nomeArquivo = pdfUrl.substring(pdfUrl.lastIndexOf('/') + 1);
    return pdfGeracaoService
        .lerPdf(nomeArquivo)
        .map(
            bytes ->
                ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nomeArquivo + "\"")
                    .body(bytes))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  private String extrairFormato(MultipartFile audio) {
    String nomeOriginal = audio.getOriginalFilename();
    if (nomeOriginal != null && nomeOriginal.contains(".")) {
      return nomeOriginal.substring(nomeOriginal.lastIndexOf('.') + 1);
    }
    String contentType = audio.getContentType();
    if (contentType != null && contentType.contains("/")) {
      return contentType.substring(contentType.lastIndexOf('/') + 1);
    }
    return "ogg";
  }
}
