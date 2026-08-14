package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.domain.exception.AlunoNaoAssociadoException;
import com.escolademusica.relatorios.domain.exception.ConflitoException;
import com.escolademusica.relatorios.domain.exception.CredenciaisInvalidasException;
import com.escolademusica.relatorios.domain.exception.PeriodoSemRelatoriosException;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.ErroDto;
import com.escolademusica.relatorios.gateway.AudioNaoProcessavelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tratamento centralizado de excecoes, produzindo sempre o DTO padrao {@link ErroDto} ({@code
 * {codigo, mensagem}}). Nunca inclui stack trace no corpo da resposta; detalhes tecnicos completos
 * sao apenas logados no servidor.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** Erros de validacao de payload (Bean Validation em @RequestBody). */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErroDto> tratarValidacao(MethodArgumentNotValidException ex) {
    String mensagem =
        ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .orElse("Dados invalidos");
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErroDto("DADOS_INVALIDOS", mensagem));
  }

  /** Corpo da requisicao malformado (JSON invalido, tipos incompativeis). */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErroDto> tratarCorpoInvalido(HttpMessageNotReadableException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErroDto("DADOS_INVALIDOS", "Corpo da requisicao invalido ou malformado"));
  }

  /** Erros de argumento explicitos lancados pela camada de dominio/usecase. */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErroDto> tratarArgumentoInvalido(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErroDto("DADOS_INVALIDOS", ex.getMessage()));
  }

  /** Recurso solicitado nao existe. */
  @ExceptionHandler(RecursoNaoEncontradoException.class)
  public ResponseEntity<ErroDto> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ErroDto("RECURSO_NAO_ENCONTRADO", ex.getMessage()));
  }

  /** Audio nao processavel (corrompido/formato invalido/silencio) — nao cria relatorio (T050). */
  @ExceptionHandler(AudioNaoProcessavelException.class)
  public ResponseEntity<ErroDto> tratarAudioNaoProcessavel(AudioNaoProcessavelException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(new ErroDto("AUDIO_NAO_PROCESSAVEL", ex.getMessage()));
  }

  /** Periodo sem nenhum relatorio de aula aprovado — nao gera relatorio semestral (FR-014). */
  @ExceptionHandler(PeriodoSemRelatoriosException.class)
  public ResponseEntity<ErroDto> tratarPeriodoSemRelatorios(PeriodoSemRelatoriosException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(new ErroDto("PERIODO_SEM_RELATORIOS", ex.getMessage()));
  }

  /** Aluno nao associado ao professor solicitante (FR-018). */
  @ExceptionHandler(AlunoNaoAssociadoException.class)
  public ResponseEntity<ErroDto> tratarAlunoNaoAssociado(AlunoNaoAssociadoException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ErroDto("ALUNO_NAO_ASSOCIADO", ex.getMessage()));
  }

  /** Conflito de estado (ex.: transicao de status invalida, duplicidade). */
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErroDto> tratarConflito(IllegalStateException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErroDto("CONFLITO", ex.getMessage()));
  }

  /** Conflito de dominio com codigo de erro estavel (ex.: ALUNO_CPF_DUPLICADO). */
  @ExceptionHandler(ConflitoException.class)
  public ResponseEntity<ErroDto> tratarConflitoDominio(ConflitoException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErroDto(ex.getCodigo(), ex.getMessage()));
  }

  /** Credenciais de administrador invalidas (login). */
  @ExceptionHandler(CredenciaisInvalidasException.class)
  public ResponseEntity<ErroDto> tratarCredenciaisInvalidas(CredenciaisInvalidasException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErroDto("CREDENCIAIS_INVALIDAS", ex.getMessage()));
  }

  /** Fallback generico — nunca expor stack trace ou mensagem interna crua ao cliente. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErroDto> tratarErroGenerico(Exception ex) {
    log.error("Erro nao tratado", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            new ErroDto("ERRO_INTERNO", "Ocorreu um erro inesperado. Tente novamente mais tarde."));
  }
}
