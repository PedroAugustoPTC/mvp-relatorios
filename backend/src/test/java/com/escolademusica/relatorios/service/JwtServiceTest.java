package com.escolademusica.relatorios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
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
  void construtorDeveLancarQuandoJanelaDeSessaoDoProfessorNaoForPositiva() {
    assertThatThrownBy(() -> new JwtService("segredo-de-teste-suficientemente-longo", 0))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new JwtService("segredo-de-teste-suficientemente-longo", -1))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void gerarTokenProfessorDeveProduzirTokenValidavelComOIdDoProfessor() {
    UUID professorId = UUID.randomUUID();

    JwtService.TokenGerado gerado = jwtService.gerarTokenProfessor(professorId);

    assertThat(gerado.token()).isNotBlank();
    assertThat(gerado.expiraEm()).isAfter(java.time.OffsetDateTime.now());
    assertThat(jwtService.validarEExtrairProfessorId(gerado.token())).contains(professorId);
  }

  @Test
  void expiracaoDoTokenDeProfessorDeveSeguirAJanelaConfigurada() {
    JwtService comJanelaDe1h = new JwtService("segredo-de-teste-suficientemente-longo", 1);

    assertThat(comJanelaDe1h.getExpiracaoProfessor()).isEqualTo(Duration.ofHours(1));
    assertThat(comJanelaDe1h.gerarTokenProfessor(UUID.randomUUID()).expiraEm())
        .isBefore(java.time.OffsetDateTime.now().plusHours(2));
    assertThat(jwtService.getExpiracaoProfessor()).isEqualTo(Duration.ofHours(12));
  }

  @Test
  void sessaoDeslizanteDeveEmitirNovoTokenComExpiracaoRenovadaACadaChamada() throws Exception {
    UUID professorId = UUID.randomUUID();

    JwtService.TokenGerado primeiro = jwtService.gerarTokenProfessor(professorId);
    Thread.sleep(1100); // segundos sao a menor granularidade de `exp` em JWT
    JwtService.TokenGerado renovado = jwtService.gerarTokenProfessor(professorId);

    assertThat(renovado.expiraEm()).isAfter(primeiro.expiraEm());
    assertThat(jwtService.validarEExtrairProfessorId(renovado.token())).contains(professorId);
  }

  @Test
  void tokenDeProfessorNaoDeveAutenticarAInterfaceAdministrativa() {
    String tokenProfessor = jwtService.gerarTokenProfessor(UUID.randomUUID()).token();

    assertThat(jwtService.validarEExtrairSubject(tokenProfessor)).isEmpty();
  }

  @Test
  void tokenDeAdministradorNaoDeveAutenticarOPortalDoProfessor() {
    String tokenAdmin = jwtService.gerarToken("admin@escola.com").token();

    assertThat(jwtService.validarEExtrairProfessorId(tokenAdmin)).isEmpty();
  }

  @Test
  void validarEExtrairProfessorIdDeveRetornarVazioParaTokenInvalidoOuSubjectNaoUuid() {
    assertThat(jwtService.validarEExtrairProfessorId("token-invalido")).isEmpty();

    byte[] chaveBytes = derivarChaveDoSegredoDeTeste();
    String tokenComSubjectNaoUuid =
        Jwts.builder()
            .subject("nao-e-um-uuid")
            .claim(JwtService.CLAIM_ROLE, JwtService.ROLE_PROFESSOR)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(60)))
            .signWith(Keys.hmacShaKeyFor(chaveBytes))
            .compact();

    assertThat(jwtService.validarEExtrairProfessorId(tokenComSubjectNaoUuid)).isEmpty();
  }

  @Test
  void validarEExtrairProfessorIdDeveRetornarVazioParaTokenExpirado() {
    String tokenExpirado =
        Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim(JwtService.CLAIM_ROLE, JwtService.ROLE_PROFESSOR)
            .issuedAt(Date.from(Instant.now().minus(Duration.ofHours(13))))
            .expiration(Date.from(Instant.now().minus(Duration.ofHours(1))))
            .signWith(Keys.hmacShaKeyFor(derivarChaveDoSegredoDeTeste()))
            .compact();

    assertThat(jwtService.validarEExtrairProfessorId(tokenExpirado)).isEmpty();
  }

  private static byte[] derivarChaveDoSegredoDeTeste() {
    try {
      return java.security.MessageDigest.getInstance("SHA-256")
          .digest("segredo-de-teste-suficientemente-longo".getBytes());
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
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
