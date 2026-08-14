package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.dto.LoginRequestDto;
import com.escolademusica.relatorios.dto.LoginResponseDto;
import com.escolademusica.relatorios.service.JwtService;
import com.escolademusica.relatorios.usecase.AutenticarAdminUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Autenticacao do administrador da interface web ({@code /api/v1/auth}, T061). */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacao", description = "Login do administrador da interface web")
public class AuthController {

  private final AutenticarAdminUseCase autenticarAdminUseCase;

  public AuthController(AutenticarAdminUseCase autenticarAdminUseCase) {
    this.autenticarAdminUseCase = autenticarAdminUseCase;
  }

  @Operation(summary = "Autentica o administrador e emite um token JWT")
  @PostMapping("/login")
  public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto) {
    JwtService.TokenGerado token =
        autenticarAdminUseCase.executar(requestDto.email(), requestDto.senha());
    return ResponseEntity.ok(new LoginResponseDto(token.token(), token.expiraEm()));
  }
}
