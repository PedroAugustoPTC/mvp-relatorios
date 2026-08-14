package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.dto.AutenticacaoProfessorResponseDto;
import com.escolademusica.relatorios.dto.AutenticacaoProfessorResponseDto.ProfessorAutenticadoDto;
import com.escolademusica.relatorios.dto.VincularCodigoRequestDto;
import com.escolademusica.relatorios.usecase.AutenticarProfessorPortalUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticacao do professor no portal web (spec 002, US2).
 *
 * <p>Unico endpoint publico sob {@code /api/v1/professor/**} — e por onde a sessao nasce, entao nao
 * pode exigir um token. A protecao contra tentativas automatizadas vem do rate limit por origem
 * (FR-004a), aplicado dentro do UseCase.
 */
@RestController
@Tag(
    name = "Portal do professor - Autenticacao",
    description = "Login do professor no portal web pelo codigo de vinculacao")
public class ProfessorAuthController {

  private final AutenticarProfessorPortalUseCase autenticarProfessorPortalUseCase;

  public ProfessorAuthController(
      AutenticarProfessorPortalUseCase autenticarProfessorPortalUseCase) {
    this.autenticarProfessorPortalUseCase = autenticarProfessorPortalUseCase;
  }

  /** {@code POST /api/v1/professor/auth/vincular} */
  @Operation(
      summary =
          "Autentica o professor pelo codigo de vinculacao e emite a sessao do portal (FR-003)")
  @PostMapping("/api/v1/professor/auth/vincular")
  public AutenticacaoProfessorResponseDto vincular(
      @Valid @RequestBody VincularCodigoRequestDto corpo, HttpServletRequest request) {
    AutenticarProfessorPortalUseCase.Resultado resultado =
        autenticarProfessorPortalUseCase.autenticar(
            identificadorDaOrigem(request), corpo.codigoVinculacao());

    return new AutenticacaoProfessorResponseDto(
        resultado.token(),
        resultado.expiraEm(),
        new ProfessorAutenticadoDto(resultado.professorId(), resultado.nomeProfessor()));
  }

  /**
   * Identificador da origem usado pelo rate limit: hash SHA-256 do IP de origem.
   *
   * <p>O IP nunca e persistido em claro (Principio V/LGPD) — o objetivo e apenas distinguir origens
   * entre si para contar tentativas, nao registrar de onde cada professor acessa o sistema.
   */
  private String identificadorDaOrigem(HttpServletRequest request) {
    String ip = request.getRemoteAddr();
    if (ip == null || ip.isBlank()) {
      ip = "desconhecido";
    }
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(ip.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Falha ao derivar o identificador de origem", e);
    }
  }
}
