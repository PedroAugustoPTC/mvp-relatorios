package com.escolademusica.relatorios.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Emissao e validacao de JWT curto prazo para a sessao do administrador (T058, research.md secao
 * 6). A chave configurada em {@code security.jwt.secret} (variavel de ambiente {@code JWT_SECRET})
 * e normalizada via SHA-256 para produzir sempre uma chave HMAC-SHA256 de 256 bits valida,
 * tolerando segredos textuais curtos em ambientes de desenvolvimento (mesmo padrao adotado por
 * {@link CriptografiaService}).
 */
@Service
public class JwtService {

  /** Tempo de vida do token emitido no login (JWT de curta duracao, ver research.md secao 6). */
  public static final Duration EXPIRACAO = Duration.ofHours(2);

  /**
   * Nome da claim que identifica o papel do portador do token. Ausente nos tokens de administrador
   * (spec 001) e igual a {@link #ROLE_PROFESSOR} nos tokens do portal do professor (spec 002).
   */
  public static final String CLAIM_ROLE = "role";

  /** Valor de {@link #CLAIM_ROLE} nos tokens de sessao do portal do professor. */
  public static final String ROLE_PROFESSOR = "PROFESSOR";

  /** Janela default de inatividade da sessao do portal do professor (FR-005). */
  public static final Duration EXPIRACAO_PROFESSOR_PADRAO = Duration.ofHours(12);

  private final SecretKey chaveAssinatura;
  private final Duration expiracaoProfessor;

  /**
   * Conveniencia (testes e uso programatico): adota a janela default de sessao do professor. Nao e
   * anotado com {@code @Value} de proposito — o Spring injeta pelo construtor marcado com {@link
   * Autowired} abaixo, que le a configuracao.
   */
  public JwtService(String segredo) {
    this(segredo, (int) EXPIRACAO_PROFESSOR_PADRAO.toHours());
  }

  @Autowired
  public JwtService(
      @Value("${security.jwt.secret}") String segredo,
      @Value("${security.jwt.professor-session-hours:12}") int horasSessaoProfessor) {
    if (segredo == null || segredo.isBlank()) {
      throw new IllegalStateException("security.jwt.secret (JWT_SECRET) nao configurada");
    }
    if (horasSessaoProfessor <= 0) {
      throw new IllegalStateException(
          "security.jwt.professor-session-hours deve ser maior que zero: " + horasSessaoProfessor);
    }
    this.chaveAssinatura = Keys.hmacShaKeyFor(derivarChave256Bits(segredo));
    this.expiracaoProfessor = Duration.ofHours(horasSessaoProfessor);
  }

  private static byte[] derivarChave256Bits(String segredo) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(segredo.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Falha ao derivar chave de assinatura JWT", e);
    }
  }

  /** Token gerado para o administrador autenticado, com o instante exato de expiracao. */
  public record TokenGerado(String token, OffsetDateTime expiraEm) {}

  /** Gera um token assinado com o e-mail do administrador como subject. */
  public TokenGerado gerarToken(String emailAdministrador) {
    Instant agora = Instant.now();
    Instant expiracao = agora.plus(EXPIRACAO);
    String token =
        Jwts.builder()
            .subject(emailAdministrador)
            .issuedAt(Date.from(agora))
            .expiration(Date.from(expiracao))
            .signWith(chaveAssinatura)
            .compact();
    return new TokenGerado(token, expiracao.atOffset(ZoneOffset.UTC));
  }

  /**
   * Valida assinatura e expiracao do token e retorna o e-mail do administrador (subject). Retorna
   * {@link Optional#empty()} quando o token e invalido, expirado ou malformado — nunca lanca para o
   * chamador tratar como falha de autenticacao (401), nao como erro interno.
   */
  public Optional<String> validarEExtrairSubject(String token) {
    return lerClaims(token)
        // Um token de professor NUNCA autentica a interface administrativa: sem esta checagem, o
        // JWT emitido para o portal (assinado com a mesma chave) seria aceito pela cadeia
        // /api/v1/** como se fosse um administrador (T010, Principio V).
        .filter(claims -> !ROLE_PROFESSOR.equals(claims.get(CLAIM_ROLE, String.class)))
        .map(Claims::getSubject);
  }

  /**
   * Emite o token de sessao do portal do professor (spec 002, FR-005): {@code sub=professorId},
   * {@code role=PROFESSOR} e expiracao igual a janela de inatividade configurada. A sessao e
   * deslizante — {@code ProfessorJwtAuthenticationFilter} reemite um token novo a cada requisicao
   * autenticada, de modo que a expiracao so e efetivamente atingida apos inatividade real.
   */
  public TokenGerado gerarTokenProfessor(UUID professorId) {
    Instant agora = Instant.now();
    Instant expiracao = agora.plus(expiracaoProfessor);
    String token =
        Jwts.builder()
            .subject(professorId.toString())
            .claim(CLAIM_ROLE, ROLE_PROFESSOR)
            .issuedAt(Date.from(agora))
            .expiration(Date.from(expiracao))
            .signWith(chaveAssinatura)
            .compact();
    return new TokenGerado(token, expiracao.atOffset(ZoneOffset.UTC));
  }

  /**
   * Valida assinatura, expiracao e a claim {@code role=PROFESSOR}, retornando o id do professor
   * (subject). Retorna {@link Optional#empty()} para token invalido/expirado/malformado, para
   * subject que nao seja um UUID e tambem para um token de administrador — que nao da acesso ao
   * portal do professor.
   */
  public Optional<UUID> validarEExtrairProfessorId(String token) {
    return lerClaims(token)
        .filter(claims -> ROLE_PROFESSOR.equals(claims.get(CLAIM_ROLE, String.class)))
        .map(Claims::getSubject)
        .flatMap(JwtService::paraUuid);
  }

  /** Janela de inatividade vigente da sessao do portal do professor. */
  public Duration getExpiracaoProfessor() {
    return expiracaoProfessor;
  }

  private Optional<Claims> lerClaims(String token) {
    try {
      return Optional.ofNullable(
          Jwts.parser().verifyWith(chaveAssinatura).build().parseSignedClaims(token).getPayload());
    } catch (JwtException | IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  private static Optional<UUID> paraUuid(String valor) {
    try {
      return Optional.of(UUID.fromString(valor));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }
}
