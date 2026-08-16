package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.domain.CanalOrigem;
import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.AprovarRelatorioRequestDto;
import com.escolademusica.relatorios.dto.AprovarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.EnviarAudioPortalResponseDto;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.ResponderPerguntaRequestDto;
import com.escolademusica.relatorios.dto.RevisarRelatorioRequestDto;
import com.escolademusica.relatorios.mapper.RelatorioAulaMapper;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.service.ControleAcessoProfessorService;
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
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Fluxo de relatorio de aula pelo portal web (spec 002, US1): enviar audio -> acompanhar ->
 * responder pergunta -> revisar -> aprovar/cancelar.
 *
 * <p><b>Nenhuma regra de negocio nova vive aqui</b> (Principio I): este controller e apenas um
 * adaptador de entrada do canal web sobre os mesmos UseCases ja usados pelo bot do Telegram ({@code
 * RegistrarAulaUseCase}, {@code EstruturarRelatorioUseCase}, {@code ResponderPerguntaUseCase},
 * {@code RevisarRelatorioAulaUseCase}, {@code AprovarRelatorioAulaUseCase}). A unica coisa
 * especifica do canal e gravar {@code canalOrigem=WEB}.
 *
 * <p><b>Sobre o 202 do envio de audio:</b> o contrato prevê 202 + polling porque transcricao e
 * estruturacao levam alguns segundos. A transcricao, porem, precisa concluir ANTES de existir um
 * {@code relatorioId} para devolver — {@code RegistrarAulaUseCase} so persiste a aula/relatorio
 * apos o audio ser transcrito com sucesso, exatamente para nao deixar registros orfaos quando o
 * audio e inutilizavel (FR-011). Por isso o processamento acontece dentro da requisicao e o 202
 * carrega ja o status real; o frontend mantem o polling em {@code GET .../{relatorioId}}, que
 * continua sendo a fonte de verdade do andamento (e o caminho usado apos responder perguntas).
 *
 * <p><b>Retencao de audio (FR-009):</b> o arquivo recebido e lido para memoria, repassado a
 * transcricao e descartado — nada e escrito em disco nem persistido em banco, mesma politica ja
 * aplicada ao canal Telegram.
 */
@RestController
@RequestMapping("/api/v1/professor")
@Tag(
    name = "Portal do professor - Relatorio de aula",
    description = "Envio de audio, acompanhamento, revisao e aprovacao pelo portal web")
public class ProfessorPortalRelatorioAulaController {

  private final ControleAcessoProfessorService controleAcesso;
  private final RegistrarAulaUseCase registrarAulaUseCase;
  private final EstruturarRelatorioUseCase estruturarRelatorioUseCase;
  private final ResponderPerguntaUseCase responderPerguntaUseCase;
  private final RevisarRelatorioAulaUseCase revisarRelatorioAulaUseCase;
  private final AprovarRelatorioAulaUseCase aprovarRelatorioAulaUseCase;
  private final CancelarRelatorioAulaUseCase cancelarRelatorioAulaUseCase;
  private final RelatorioAulaRepository relatorioAulaRepository;
  private final RelatorioAulaMapper mapper;

  public ProfessorPortalRelatorioAulaController(
      ControleAcessoProfessorService controleAcesso,
      RegistrarAulaUseCase registrarAulaUseCase,
      EstruturarRelatorioUseCase estruturarRelatorioUseCase,
      ResponderPerguntaUseCase responderPerguntaUseCase,
      RevisarRelatorioAulaUseCase revisarRelatorioAulaUseCase,
      AprovarRelatorioAulaUseCase aprovarRelatorioAulaUseCase,
      CancelarRelatorioAulaUseCase cancelarRelatorioAulaUseCase,
      RelatorioAulaRepository relatorioAulaRepository,
      RelatorioAulaMapper mapper) {
    this.controleAcesso = controleAcesso;
    this.registrarAulaUseCase = registrarAulaUseCase;
    this.estruturarRelatorioUseCase = estruturarRelatorioUseCase;
    this.responderPerguntaUseCase = responderPerguntaUseCase;
    this.revisarRelatorioAulaUseCase = revisarRelatorioAulaUseCase;
    this.aprovarRelatorioAulaUseCase = aprovarRelatorioAulaUseCase;
    this.cancelarRelatorioAulaUseCase = cancelarRelatorioAulaUseCase;
    this.relatorioAulaRepository = relatorioAulaRepository;
    this.mapper = mapper;
  }

  /** {@code POST /api/v1/professor/alunos/{alunoId}/relatorios-aula/audio} (multipart) */
  @Operation(
      summary = "Envia o audio de uma aula gravado ou enviado pelo portal web (FR-006 a FR-009)")
  @PostMapping(
      value = "/alunos/{alunoId}/relatorios-aula/audio",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public EnviarAudioPortalResponseDto enviarAudio(
      @AuthenticationPrincipal UUID professorId,
      @PathVariable UUID alunoId,
      @RequestParam MultipartFile audio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dataAula) {
    controleAcesso.exigirAlunoDoProfessor(professorId, alunoId);

    byte[] audioBytes;
    try {
      audioBytes = audio.getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("Falha ao ler o arquivo de audio enviado", e);
    }

    RegistrarAulaUseCase.Resultado resultado =
        registrarAulaUseCase.registrar(
            professorId,
            alunoId,
            dataAula != null ? dataAula : LocalDate.now(),
            audioBytes,
            extrairFormato(audio));

    marcarCanalWeb(resultado.aulaId());

    EstruturarRelatorioResponseDto estruturado =
        estruturarRelatorioUseCase.estruturar(resultado.aulaId());
    return new EnviarAudioPortalResponseDto(
        estruturado.relatorioId(), estruturado.status(), estruturado.perguntasPendentes());
  }

  /** {@code GET /api/v1/professor/relatorios-aula/{relatorioId}} */
  @Operation(summary = "Estado atual do relatorio de aula: campos estruturados, status e pdfUrl")
  @GetMapping("/relatorios-aula/{relatorioId}")
  public EstruturarRelatorioResponseDto consultar(
      @AuthenticationPrincipal UUID professorId, @PathVariable UUID relatorioId) {
    RelatorioAula relatorio =
        controleAcesso.exigirRelatorioAulaDoProfessor(professorId, relatorioId);
    return mapper.paraResponseDto(relatorio, mapper.perguntasPendentesDe(relatorio));
  }

  /** {@code POST /api/v1/professor/relatorios-aula/{relatorioId}/responder-pergunta} */
  @Operation(summary = "Responde a uma pergunta de informacao faltante (FR-010)")
  @PostMapping("/relatorios-aula/{relatorioId}/responder-pergunta")
  public EstruturarRelatorioResponseDto responderPergunta(
      @AuthenticationPrincipal UUID professorId,
      @PathVariable UUID relatorioId,
      @Valid @RequestBody ResponderPerguntaRequestDto corpo) {
    controleAcesso.exigirRelatorioAulaDoProfessor(professorId, relatorioId);
    return responderPerguntaUseCase.responder(relatorioId, corpo.resposta());
  }

  /** {@code POST /api/v1/professor/relatorios-aula/{relatorioId}/revisar} */
  @Operation(summary = "Solicita alteracao em linguagem natural, gerando nova versao (FR-013)")
  @PostMapping("/relatorios-aula/{relatorioId}/revisar")
  public EstruturarRelatorioResponseDto revisar(
      @AuthenticationPrincipal UUID professorId,
      @PathVariable UUID relatorioId,
      @Valid @RequestBody RevisarRelatorioRequestDto corpo) {
    controleAcesso.exigirRelatorioAulaDoProfessor(professorId, relatorioId);
    return revisarRelatorioAulaUseCase.revisar(relatorioId, corpo.instrucao());
  }

  /** {@code POST /api/v1/professor/relatorios-aula/{relatorioId}/aprovar} */
  @Operation(summary = "Aprova explicitamente a versao vigente do relatorio (FR-012)")
  @PostMapping("/relatorios-aula/{relatorioId}/aprovar")
  public AprovarRelatorioResponseDto aprovar(
      @AuthenticationPrincipal UUID professorId,
      @PathVariable UUID relatorioId,
      @Valid @RequestBody AprovarRelatorioRequestDto corpo) {
    controleAcesso.exigirRelatorioAulaDoProfessor(professorId, relatorioId);
    return aprovarRelatorioAulaUseCase.aprovar(relatorioId, corpo.versaoConfirmada());
  }

  /** {@code POST /api/v1/professor/relatorios-aula/{relatorioId}/cancelar} */
  @Operation(summary = "Cancela o rascunho quando o professor decide nao seguir")
  @PostMapping("/relatorios-aula/{relatorioId}/cancelar")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelar(@AuthenticationPrincipal UUID professorId, @PathVariable UUID relatorioId) {
    controleAcesso.exigirRelatorioAulaDoProfessor(professorId, relatorioId);
    cancelarRelatorioAulaUseCase.cancelar(relatorioId);
  }

  /**
   * Marca o relatorio recem-criado como originado no portal web. Feito aqui, no adaptador do canal,
   * e nao dentro do UseCase — que permanece agnostico a canal (FR-002).
   */
  private void marcarCanalWeb(UUID aulaId) {
    RelatorioAula relatorio =
        relatorioAulaRepository
            .findByAulaId(aulaId)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Relatorio de aula nao encontrado para aulaId=" + aulaId));
    relatorio.setCanalOrigem(CanalOrigem.WEB);
    relatorio.setAtualizadoEm(OffsetDateTime.now());
    relatorioAulaRepository.save(relatorio);
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
    // O MediaRecorder do navegador produz webm por padrao; o bot do Telegram, ogg.
    return "webm";
  }
}
