package com.escolademusica.relatorios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CriptografiaServiceTest {

  private CriptografiaService criptografiaService;

  @BeforeEach
  void setUp() {
    // Chave AES-256 valida em Base64, apenas para teste (nao usada em nenhum ambiente real).
    byte[] chaveBytes = new byte[32];
    for (int i = 0; i < chaveBytes.length; i++) {
      chaveBytes[i] = (byte) i;
    }
    String chaveBase64 = Base64.getEncoder().encodeToString(chaveBytes);
    criptografiaService = new CriptografiaService(chaveBase64);
  }

  @Test
  void deveDescriptografarParaOMesmoCpfOriginal() {
    String cpf = "12345678901";

    byte[] cifrado = criptografiaService.encrypt(cpf);
    String decifrado = criptografiaService.decrypt(cifrado);

    assertThat(decifrado).isEqualTo(cpf);
  }

  @Test
  void cifraDeveSerDiferenteACadaChamadaMasDescriptografarParaOMesmoValor() {
    String cpf = "12345678901";

    byte[] cifrado1 = criptografiaService.encrypt(cpf);
    byte[] cifrado2 = criptografiaService.encrypt(cpf);

    // IV aleatorio garante ciphertexts distintos mesmo para o mesmo CPF.
    assertThat(cifrado1).isNotEqualTo(cifrado2);
    assertThat(criptografiaService.decrypt(cifrado1)).isEqualTo(cpf);
    assertThat(criptografiaService.decrypt(cifrado2)).isEqualTo(cpf);
  }

  @Test
  void hashDeveSerDeterministicoParaOMesmoCpf() {
    String cpf = "12345678901";

    String hash1 = criptografiaService.hash(cpf);
    String hash2 = criptografiaService.hash(cpf);

    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  void hashDeveSerDiferenteParaCpfsDiferentes() {
    String hashA = criptografiaService.hash("12345678901");
    String hashB = criptografiaService.hash("98765432100");

    assertThat(hashA).isNotEqualTo(hashB);
  }

  @Test
  void cpfFormatadoDeveGerarOMesmoHashQueCpfSemFormatacao() {
    String semFormatacao = "12345678901";
    String formatado = "123.456.789-01";

    assertThat(criptografiaService.hash(formatado))
        .isEqualTo(criptografiaService.hash(semFormatacao));
  }

  @Test
  void cpfFormatadoDeveDescriptografarParaOsMesmosDigitosQueCpfSemFormatacao() {
    String semFormatacao = "12345678901";
    String formatado = "123.456.789-01";

    byte[] cifradoFormatado = criptografiaService.encrypt(formatado);
    byte[] cifradoSemFormatacao = criptografiaService.encrypt(semFormatacao);

    assertThat(criptografiaService.decrypt(cifradoFormatado)).isEqualTo(semFormatacao);
    assertThat(criptografiaService.decrypt(cifradoSemFormatacao)).isEqualTo(semFormatacao);
  }

  @Test
  void normalizarDeveRemoverCaracteresNaoNumericos() {
    assertThat(CriptografiaService.normalizar("123.456.789-01")).isEqualTo("12345678901");
    assertThat(CriptografiaService.normalizar(null)).isNull();
  }

  @Test
  void deveLancarQuandoChaveConfiguradaNulaOuEmBranco() {
    assertThatThrownBy(() -> new CriptografiaService(null))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new CriptografiaService("   "))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void deveDerivarChaveValidaAPartirDeSegredoTextualNaoBase64Aes() {
    // Segredo textual arbitrario (nao Base64 de 16/24/32 bytes decodificados): deve cair no
    // fallback de derivacao via SHA-256 em vez de lancar erro.
    CriptografiaService servico = new CriptografiaService("segredo-textual-qualquer-de-dev");

    byte[] cifrado = servico.encrypt("12345678901");
    String decifrado = servico.decrypt(cifrado);

    assertThat(decifrado).isEqualTo("12345678901");
  }

  @Test
  void deveDerivarChaveValidaAPartirDeBase64ValidoComTamanhoIncorreto() {
    // Base64 valido, mas decodifica para um tamanho diferente de 16/24/32 bytes: deve cair no
    // fallback de derivacao via SHA-256 (mesmo caminho de um segredo textual comum).
    String base64DezBytes = Base64.getEncoder().encodeToString(new byte[10]);

    CriptografiaService servico = new CriptografiaService(base64DezBytes);
    byte[] cifrado = servico.encrypt("12345678901");

    assertThat(servico.decrypt(cifrado)).isEqualTo("12345678901");
  }

  @Test
  void decryptDeveLancarQuandoConteudoCifradoInvalido() {
    assertThatThrownBy(() -> criptografiaService.decrypt(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> criptografiaService.decrypt(new byte[] {1, 2, 3}))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
