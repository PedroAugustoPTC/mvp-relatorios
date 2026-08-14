package com.escolademusica.relatorios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService("segredo-de-teste-suficientemente-longo");
  }

  @Test
  void construtorDeveLancarQuandoSegredoNuloOuVazio() {
    assertThatThrownBy(() -> new JwtService(null)).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new JwtService("   ")).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void gerarTokenDeveProduzirTokenValidavel() {
    JwtService.TokenGerado gerado = jwtService.gerarToken("admin@escola.com");

    assertThat(gerado.token()).isNotBlank();
    assertThat(gerado.expiraEm()).isAfter(java.time.OffsetDateTime.now());

    Optional<String> subject = jwtService.validarEExtrairSubject(gerado.token());
    assertThat(subject).contains("admin@escola.com");
  }

  @Test
  void validarEExtrairSubjectDeveRetornarVazioParaTokenMalformado() {
    Optional<String> subject = jwtService.validarEExtrairSubject("token-invalido");

    assertThat(subject).isEmpty();
  }

  @Test
  void validarEExtrairSubjectDeveRetornarVazioParaTokenAssinadoComOutraChave() {
    SecretKey outraChave = Keys.hmacShaKeyFor(new byte[32]);
    String tokenComOutraAssinatura =
        Jwts.builder()
            .subject("outro@escola.com")
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(60)))
            .signWith(outraChave)
            .compact();

    Optional<String> subject = jwtService.validarEExtrairSubject(tokenComOutraAssinatura);

    assertThat(subject).isEmpty();
  }

  @Test
  void validarEExtrairSubjectDeveRetornarVazioParaTokenExpirado() throws Exception {
    // Deriva a mesma chave HMAC que o JwtService usa internamente (SHA-256 do segredo), para
    // assinar diretamente um token ja expirado.
    byte[] chaveBytes =
        java.security.MessageDigest.getInstance("SHA-256")
            .digest("segredo-de-teste-suficientemente-longo".getBytes());
    SecretKey chave = Keys.hmacShaKeyFor(chaveBytes);
    String tokenExpirado =
        Jwts.builder()
            .subject("admin@escola.com")
            .issuedAt(Date.from(Instant.now().minus(Duration.ofHours(3))))
            .expiration(Date.from(Instant.now().minus(Duration.ofHours(1))))
            .signWith(chave)
            .compact();

    Optional<String> subject = jwtService.validarEExtrairSubject(tokenExpirado);

    assertThat(subject).isEmpty();
  }
}
