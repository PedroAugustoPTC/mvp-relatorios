package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.VinculoTelegram;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.HistoricoAlunoResponseDto;
import com.escolademusica.relatorios.dto.VincularTelegramRequestDto;
import com.escolademusica.relatorios.dto.VincularTelegramResponseDto;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.repository.VinculoTelegramRepository;
import com.escolademusica.relatorios.usecase.ConsultarHistoricoUseCase;
import com.escolademusica.relatorios.usecase.VincularContaTelegramUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos chamados pelo workflow de vinculacao Telegram do n8n ({@code
 * /internal/v1/telegram/**}, T062). Protegidos por {@code X-N8N-Service-Token} (ver
 * SecurityConfig), nunca por autenticacao de administrador.
 *
 * <p><b>FR-020 (T064):</b> este controller expoe apenas o que contracts/api-n8n-integration.md
 * define para a vinculacao Telegram-Professor (consultar/estabelecer o vinculo). O {@code
 * telegramUserId} nunca e tratado como prova de identidade suficiente para acoes administrativas
 * sensiveis (cadastro/edicao de professores ou alunos) — essas ficam exclusivamente em {@code
 * /api/v1/**}, atras de autenticacao JWT de administrador. Nenhuma operacao de CRUD administrativo
 * e exposta aqui.
 */
@RestController
@RequestMapping("/internal/v1")
@Tag(name = "n8n - Telegram", description = "Vinculacao Telegram/professor e historico via bot")
public class TelegramWebhookController {

  private final VincularContaTelegramUseCase vincularContaTelegramUseCase;
  private final VinculoTelegramRepository vinculoTelegramRepository;
  private final ProfessorRepository professorRepository;
  private final ConsultarHistoricoUseCase consultarHistoricoUseCase;

  public TelegramWebhookController(
      VincularContaTelegramUseCase vincularContaTelegramUseCase,
      VinculoTelegramRepository vinculoTelegramRepository,
      ProfessorRepository professorRepository,
      ConsultarHistoricoUseCase consultarHistoricoUseCase) {
    this.vincularContaTelegramUseCase = vincularContaTelegramUseCase;
    this.vinculoTelegramRepository = vinculoTelegramRepository;
    this.professorRepository = professorRepository;
    this.consultarHistoricoUseCase = consultarHistoricoUseCase;
  }

  @Operation(
      summary =
          "Vincula uma conta do Telegram a um professor via codigo de vinculacao (FR-002/FR-003)")
  @PostMapping("/telegram/vinculacao")
  public ResponseEntity<VincularTelegramResponseDto> vincular(
      @Valid @RequestBody VincularTelegramRequestDto requestDto) {
    VincularContaTelegramUseCase.ResultadoVinculacao resultado =
        vincularContaTelegramUseCase.executar(
            requestDto.telegramUserId(), requestDto.codigoVinculacao());
    return ResponseEntity.ok(
        new VincularTelegramResponseDto(resultado.professorId(), resultado.nomeProfessor()));
  }

  @Operation(
      summary = "Consulta se um usuario do Telegram ja esta vinculado a um professor (FR-003)")
  @GetMapping("/telegram/{telegramUserId}/professor")
  public ResponseEntity<VincularTelegramResponseDto> consultarVinculo(
      @PathVariable String telegramUserId) {
    VinculoTelegram vinculo =
        vinculoTelegramRepository
            .findByTelegramUserId(telegramUserId)
            .orElseThrow(
                () ->
                    new RecursoNaoEncontradoException(
                        "Conta do Telegram ainda nao vinculada a um professor"));
    Professor professor =
        professorRepository
            .findById(vinculo.getProfessorId())
            .orElseThrow(
                () -> new RecursoNaoEncontradoException("Professor nao encontrado para o vinculo"));
    return ResponseEntity.ok(
        new VincularTelegramResponseDto(professor.getId(), professor.getNome()));
  }

  /**
   * Historico de um aluno associado ao professor solicitante (US4/T087, FR-017a). Delega o controle
   * de acesso FR-018 a {@link ConsultarHistoricoUseCase}, que lanca {@code
   * AlunoNaoAssociadoException} (-> 403 {@code ALUNO_NAO_ASSOCIADO}) quando o aluno nao esta
   * associado a esse professor.
   */
  @Operation(summary = "Historico de um aluno associado ao professor solicitante (FR-017a/FR-018)")
  @GetMapping("/professores/{professorId}/alunos/{alunoId}/historico")
  public ResponseEntity<HistoricoAlunoResponseDto> historico(
      @PathVariable UUID professorId, @PathVariable UUID alunoId) {
    return ResponseEntity.ok(consultarHistoricoUseCase.executar(alunoId, professorId));
  }
}
