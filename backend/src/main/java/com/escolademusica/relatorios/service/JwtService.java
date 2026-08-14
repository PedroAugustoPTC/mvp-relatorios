package com.escolademusica.relatorios.service;

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
import javax.crypto.SecretKey;
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

  private final SecretKey chaveAssinatura;

  public JwtService(@Value("${security.jwt.secret}") String segredo) {
    if (segredo == null || segredo.isBlank()) {
      throw new IllegalStateException("security.jwt.secret (JWT_SECRET) nao configurada");
    }
    this.chaveAssinatura = Keys.hmacShaKeyFor(derivarChave256Bits(segredo));
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
    try {
      String subject =
          Jwts.parser()
              .verifyWith(chaveAssinatura)
              .build()
              .parseSignedClaims(token)
              .getPayload()
              .getSubject();
      return Optional.ofNullable(subject);
    } catch (JwtException | IllegalArgumentException ex) {
      return Optional.empty();
    }
  }
}
