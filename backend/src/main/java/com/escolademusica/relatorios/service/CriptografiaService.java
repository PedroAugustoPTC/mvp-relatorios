package com.escolademusica.relatorios.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Criptografia simetrica autenticada (AES-GCM) do CPF em repouso, mais um hash deterministico
 * (HMAC-SHA-256) usado apenas para checagem de unicidade sem descriptografar todos os registros
 * (decisao registrada em research.md secao 5).
 *
 * <p>A chave configurada em {@code security.cpf-encryption-key} (variavel de ambiente
 * CPF_ENCRYPTION_KEY) deve ter, apos decodificacao Base64, 16/24/32 bytes (AES-128/192/256). Ela e
 * usada tanto para a cifra AES-GCM quanto, derivada via SHA-256, como chave HMAC do hash
 * deterministico.
 */
@Service
public class CriptografiaService {

  private static final String ALGORITMO_CIFRA = "AES/GCM/NoPadding";
  private static final String ALGORITMO_CHAVE = "AES";
  private static final String ALGORITMO_HMAC = "HmacSHA256";
  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final int GCM_IV_LENGTH_BYTES = 12;

  private final SecretKeySpec chaveAes;
  private final SecretKeySpec chaveHmac;
  private final SecureRandom secureRandom = new SecureRandom();

  public CriptografiaService(@Value("${security.cpf-encryption-key}") String chaveConfigurada) {
    byte[] chaveBytes = decodificarChave(chaveConfigurada);
    this.chaveAes = new SecretKeySpec(chaveBytes, ALGORITMO_CHAVE);
    this.chaveHmac = derivarChaveHmac(chaveBytes);
  }

  private static byte[] decodificarChave(String chaveConfigurada) {
    if (chaveConfigurada == null || chaveConfigurada.isBlank()) {
      throw new IllegalStateException(
          "security.cpf-encryption-key (CPF_ENCRYPTION_KEY) nao configurada");
    }
    try {
      byte[] decodificada = Base64.getDecoder().decode(chaveConfigurada);
      if (decodificada.length == 16 || decodificada.length == 24 || decodificada.length == 32) {
        return decodificada;
      }
    } catch (IllegalArgumentException ignored) {
      // Nao era Base64 valido; cai para o fallback abaixo.
    }
    // Fallback: deriva uma chave AES-256 valida a partir de qualquer segredo textual via
    // SHA-256, para tolerar segredos configurados como texto simples em ambientes de dev.
    return sha256(chaveConfigurada.getBytes(StandardCharsets.UTF_8));
  }

  private static SecretKeySpec derivarChaveHmac(byte[] chaveAesBytes) {
    byte[] derivada = sha256(concatenar("hmac".getBytes(StandardCharsets.UTF_8), chaveAesBytes));
    return new SecretKeySpec(derivada, ALGORITMO_HMAC);
  }

  private static byte[] sha256(byte[] entrada) {
    try {
      return java.security.MessageDigest.getInstance("SHA-256").digest(entrada);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Falha ao calcular SHA-256", e);
    }
  }

  private static byte[] concatenar(byte[] a, byte[] b) {
    byte[] resultado = new byte[a.length + b.length];
    System.arraycopy(a, 0, resultado, 0, a.length);
    System.arraycopy(b, 0, resultado, a.length, b.length);
    return resultado;
  }

  /** Remove tudo que nao for digito, para normalizar CPFs formatados (com pontos/traco). */
  public static String normalizar(String cpf) {
    if (cpf == null) {
      return null;
    }
    return cpf.replaceAll("\\D", "");
  }

  /**
   * Cifra o CPF (apos normalizacao) com AES-GCM. O IV gerado aleatoriamente e prefixado ao
   * resultado, pois e necessario para a descriptografia e nao precisa ser mantido em sigilo.
   */
  public byte[] encrypt(String cpf) {
    String normalizado = normalizar(cpf);
    try {
      byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(ALGORITMO_CIFRA);
      cipher.init(Cipher.ENCRYPT_MODE, chaveAes, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
      byte[] textoCifrado = cipher.doFinal(normalizado.getBytes(StandardCharsets.UTF_8));

      return concatenar(iv, textoCifrado);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Falha ao criptografar CPF", e);
    }
  }

  /** Decifra um valor produzido por {@link #encrypt(String)}, retornando o CPF (so digitos). */
  public String decrypt(byte[] cifrado) {
    if (cifrado == null || cifrado.length <= GCM_IV_LENGTH_BYTES) {
      throw new IllegalArgumentException("Conteudo cifrado invalido");
    }
    try {
      byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
      System.arraycopy(cifrado, 0, iv, 0, GCM_IV_LENGTH_BYTES);
      byte[] textoCifrado = new byte[cifrado.length - GCM_IV_LENGTH_BYTES];
      System.arraycopy(cifrado, GCM_IV_LENGTH_BYTES, textoCifrado, 0, textoCifrado.length);

      Cipher cipher = Cipher.getInstance(ALGORITMO_CIFRA);
      cipher.init(Cipher.DECRYPT_MODE, chaveAes, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
      byte[] textoClaro = cipher.doFinal(textoCifrado);

      return new String(textoClaro, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Falha ao descriptografar CPF", e);
    }
  }

  /**
   * Hash HMAC-SHA-256 deterministico do CPF normalizado, usado apenas para checar unicidade
   * (FR-001a) sem expor ou precisar descriptografar o valor armazenado.
   */
  public String hash(String cpf) {
    String normalizado = normalizar(cpf);
    try {
      Mac mac = Mac.getInstance(ALGORITMO_HMAC);
      mac.init(chaveHmac);
      byte[] resultado = mac.doFinal(normalizado.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(resultado);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Falha ao calcular hash do CPF", e);
    }
  }
}
