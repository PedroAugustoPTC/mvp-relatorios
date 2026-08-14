package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.dto.AprovarRelatorioRequestDto;
import com.escolademusica.relatorios.dto.AprovarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.ResponderPerguntaRequestDto;
import com.escolademusica.relatorios.dto.RevisarRelatorioRequestDto;
import com.escolademusica.relatorios.dto.TranscreverResponseDto;
import com.escolademusica.relatorios.usecase.AprovarRelatorioAulaUseCase;
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
import java.util.UUID;
import org.springframework.http.MediaType;
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

  public RelatorioAulaController(
      RegistrarAulaUseCase registrarAulaUseCase,
      EstruturarRelatorioUseCase estruturarRelatorioUseCase,
      ResponderPerguntaUseCase responderPerguntaUseCase,
      RevisarRelatorioAulaUseCase revisarRelatorioAulaUseCase,
      AprovarRelatorioAulaUseCase aprovarRelatorioAulaUseCase) {
    this.registrarAulaUseCase = registrarAulaUseCase;
    this.estruturarRelatorioUseCase = estruturarRelatorioUseCase;
    this.responderPerguntaUseCase = responderPerguntaUseCase;
    this.revisarRelatorioAulaUseCase = revisarRelatorioAulaUseCase;
    this.aprovarRelatorioAulaUseCase = aprovarRelatorioAulaUseCase;
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
