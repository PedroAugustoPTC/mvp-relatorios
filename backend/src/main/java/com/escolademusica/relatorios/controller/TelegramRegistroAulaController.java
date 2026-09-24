package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.domain.ProfessorAluno;
import com.escolademusica.relatorios.domain.VinculoTelegram;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.RascunhoPendenteResponseDto;
import com.escolademusica.relatorios.dto.SelecionarAlunoRequestDto;
import com.escolademusica.relatorios.dto.SelecionarAlunoResponseDto;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.repository.VinculoTelegramRepository;
import com.escolademusica.relatorios.service.RelogioEscola;
import com.escolademusica.relatorios.usecase.DetectarRascunhoPendenteUseCase;
import com.escolademusica.relatorios.usecase.EstruturarRelatorioUseCase;
import com.escolademusica.relatorios.usecase.RegistrarAulaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints internos do bot do Telegram (canal exclusivo de registro de aula) que resolvem
 * professor/aluno a partir do {@code telegramUserId}, sem o n8n precisar carregar nenhum desses ids
 * entre mensagens.
 *
 * <p>Necessario porque cada mensagem do Telegram dispara uma execucao nova e isolada do n8n (um
 * {@code Telegram Trigger} nao pode "esperar" a proxima mensagem de dentro do mesmo fluxo) — todo o
 * estado da conversa (aluno selecionado, relatorio em andamento) precisa viver aqui, nunca no n8n.
 */
@RestController
@Tag(
    name = "n8n - Telegram (registro de aula)",
    description = "Selecao de aluno e envio de audio resolvidos por telegramUserId")
public class TelegramRegistroAulaController {

  private final VinculoTelegramRepository vinculoTelegramRepository;
  private final ProfessorAlunoRepository professorAlunoRepository;
  private final AlunoRepository alunoRepository;
  private final DetectarRascunhoPendenteUseCase detectarRascunhoPendenteUseCase;
  private final RegistrarAulaUseCase registrarAulaUseCase;
  private final EstruturarRelatorioUseCase estruturarRelatorioUseCase;
  private final RelogioEscola relogioEscola;

  public TelegramRegistroAulaController(
      VinculoTelegramRepository vinculoTelegramRepository,
      ProfessorAlunoRepository professorAlunoRepository,
      AlunoRepository alunoRepository,
      DetectarRascunhoPendenteUseCase detectarRascunhoPendenteUseCase,
      RegistrarAulaUseCase registrarAulaUseCase,
      EstruturarRelatorioUseCase estruturarRelatorioUseCase,
      RelogioEscola relogioEscola) {
    this.vinculoTelegramRepository = vinculoTelegramRepository;
    this.professorAlunoRepository = professorAlunoRepository;
    this.alunoRepository = alunoRepository;
    this.detectarRascunhoPendenteUseCase = detectarRascunhoPendenteUseCase;
    this.registrarAulaUseCase = registrarAulaUseCase;
    this.estruturarRelatorioUseCase = estruturarRelatorioUseCase;
    this.relogioEscola = relogioEscola;
  }

  /**
   * {@code GET /internal/v1/telegram/{telegramUserId}/rascunho-pendente} — ha um relatorio de aula
   * em andamento (qualquer aluno) para o professor vinculado a este telegramUserId? E' a primeira
   * coisa que o n8n consulta a cada mensagem recebida, ja que nao guarda em que ponto da conversa o
   * professor estava.
   */
  @Operation(
      summary = "Rascunho de relatorio de aula em aberto para o professor deste telegramUserId")
  @GetMapping("/internal/v1/telegram/{telegramUserId}/rascunho-pendente")
  public RascunhoPendenteResponseDto rascunhoPendente(@PathVariable String telegramUserId) {
    UUID professorId = resolverProfessorId(telegramUserId);
    return detectarRascunhoPendenteUseCase.detectarPorProfessor(professorId);
  }

  /**
   * {@code POST /internal/v1/telegram/{telegramUserId}/alunos/selecionar} — o professor respondeu
   * ao menu de alunos com um nome (texto livre); casa contra a lista dos alunos vinculados a ele e,
   * se achar, grava a selecao para a proxima mensagem (o envio do audio) usar.
   */
  @Operation(summary = "Seleciona o aluno da proxima aula a partir do nome digitado pelo professor")
  @PostMapping("/internal/v1/telegram/{telegramUserId}/alunos/selecionar")
  public SelecionarAlunoResponseDto selecionarAluno(
      @PathVariable String telegramUserId, @Valid @RequestBody SelecionarAlunoRequestDto corpo) {
    VinculoTelegram vinculo = resolverVinculo(telegramUserId);
    String nomeDigitado = corpo.nomeAluno().trim();

    Aluno alunoEncontrado =
        professorAlunoRepository.findByProfessorId(vinculo.getProfessorId()).stream()
            .map(ProfessorAluno::getAlunoId)
            .map(alunoRepository::findById)
            .flatMap(java.util.Optional::stream)
            .filter(aluno -> aluno.getNome().equalsIgnoreCase(nomeDigitado))
            .findFirst()
            .orElse(null);

    if (alunoEncontrado == null) {
      return new SelecionarAlunoResponseDto(false, null, null);
    }

    vinculo.setAlunoSelecionadoId(alunoEncontrado.getId());
    vinculo.setAlunoSelecionadoEm(OffsetDateTime.now());
    vinculoTelegramRepository.save(vinculo);
    return new SelecionarAlunoResponseDto(true, alunoEncontrado.getId(), alunoEncontrado.getNome());
  }

  /**
   * {@code POST /internal/v1/telegram/{telegramUserId}/relatorios-aula/audio} (multipart) — resolve
   * professor (pelo vinculo) e aluno (pela selecao gravada em {@code selecionarAluno}) sozinho; o
   * n8n so' repassa o audio recebido do Telegram.
   */
  @Operation(
      summary = "Envia o audio da aula para o aluno selecionado por este professor no Telegram")
  @PostMapping(
      value = "/internal/v1/telegram/{telegramUserId}/relatorios-aula/audio",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public EstruturarRelatorioResponseDto enviarAudio(
      @PathVariable String telegramUserId, @RequestParam MultipartFile audio) {
    VinculoTelegram vinculo = resolverVinculo(telegramUserId);
    if (vinculo.getAlunoSelecionadoId() == null) {
      throw new RecursoNaoEncontradoException(
          "Nenhum aluno selecionado para telegramUserId=" + telegramUserId);
    }

    byte[] audioBytes;
    try {
      audioBytes = audio.getBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("Falha ao ler o arquivo de audio enviado", e);
    }

    // As notas de voz do bot do Telegram sao sempre Ogg/Opus (a extensao real do arquivo, "oga",
    // nao e aceita pelo provedor de STT -- so o literal "ogg" esta na lista suportada da Groq).
    RegistrarAulaUseCase.Resultado resultado =
        registrarAulaUseCase.registrar(
            vinculo.getProfessorId(),
            vinculo.getAlunoSelecionadoId(),
            relogioEscola.hoje(),
            audioBytes,
            "ogg");

    return estruturarRelatorioUseCase.estruturar(resultado.aulaId());
  }

  private VinculoTelegram resolverVinculo(String telegramUserId) {
    return vinculoTelegramRepository
        .findByTelegramUserId(telegramUserId)
        .orElseThrow(
            () ->
                new RecursoNaoEncontradoException(
                    "Conta do Telegram ainda nao vinculada a um professor"));
  }

  private UUID resolverProfessorId(String telegramUserId) {
    return resolverVinculo(telegramUserId).getProfessorId();
  }
}
