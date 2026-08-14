package com.escolademusica.relatorios.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/** Testes das regras de bloqueio de {@link TentativaAutenticacaoWeb} (FR-004a). */
class TentativaAutenticacaoWebTest {

  private static final int MAX = 5;
  private static final int MINUTOS = 15;

  @Test
  void novaTentativaComecaSemFalhasENaoBloqueada() {
    OffsetDateTime agora = OffsetDateTime.now();
    TentativaAutenticacaoWeb tentativa = new TentativaAutenticacaoWeb("origem", agora);

    assertThat(tentativa.getTentativasIncorretas()).isZero();
    assertThat(tentativa.getBloqueadoAte()).isNull();
    assertThat(tentativa.estaBloqueadoEm(agora)).isFalse();
  }

  @Test
  void deveBloquearSomenteAoAtingirOLimite() {
    OffsetDateTime agora = OffsetDateTime.now();
    TentativaAutenticacaoWeb tentativa = new TentativaAutenticacaoWeb("origem", agora);

    for (int i = 1; i < MAX; i++) {
      tentativa.registrarFalha(agora, MAX, MINUTOS);
      assertThat(tentativa.getBloqueadoAte()).isNull();
    }

    tentativa.registrarFalha(agora, MAX, MINUTOS);

    assertThat(tentativa.getTentativasIncorretas()).isEqualTo(MAX);
    assertThat(tentativa.getBloqueadoAte()).isEqualTo(agora.plusMinutes(MINUTOS));
    assertThat(tentativa.estaBloqueadoEm(agora)).isTrue();
    assertThat(tentativa.estaBloqueadoEm(agora.plusMinutes(MINUTOS + 1))).isFalse();
  }

  @Test
  void sucessoDeveZerarContadorEBloqueio() {
    OffsetDateTime agora = OffsetDateTime.now();
    TentativaAutenticacaoWeb tentativa = new TentativaAutenticacaoWeb("origem", agora);
    tentativa.setTentativasIncorretas(MAX);
    tentativa.setBloqueadoAte(agora.plusMinutes(MINUTOS));

    tentativa.registrarSucesso(agora);

    assertThat(tentativa.getTentativasIncorretas()).isZero();
    assertThat(tentativa.getBloqueadoAte()).isNull();
    assertThat(tentativa.getAtualizadoEm()).isEqualTo(agora);
  }

  @Test
  void limparBloqueioExpiradoDeveReiniciarAContagem() {
    OffsetDateTime agora = OffsetDateTime.now();
    TentativaAutenticacaoWeb tentativa = new TentativaAutenticacaoWeb("origem", agora);
    tentativa.setTentativasIncorretas(MAX);
    tentativa.setBloqueadoAte(agora.minusMinutes(1));

    tentativa.limparBloqueioExpirado(agora);

    assertThat(tentativa.getTentativasIncorretas()).isZero();
    assertThat(tentativa.getBloqueadoAte()).isNull();
  }

  @Test
  void limparBloqueioExpiradoNaoDeveAfetarBloqueioVigenteNemRegistroSemBloqueio() {
    OffsetDateTime agora = OffsetDateTime.now();

    TentativaAutenticacaoWeb bloqueada = new TentativaAutenticacaoWeb("origem", agora);
    bloqueada.setTentativasIncorretas(MAX);
    bloqueada.setBloqueadoAte(agora.plusMinutes(5));
    bloqueada.limparBloqueioExpirado(agora);
    assertThat(bloqueada.getTentativasIncorretas()).isEqualTo(MAX);
    assertThat(bloqueada.getBloqueadoAte()).isNotNull();

    TentativaAutenticacaoWeb semBloqueio = new TentativaAutenticacaoWeb("origem", agora);
    semBloqueio.setTentativasIncorretas(2);
    semBloqueio.limparBloqueioExpirado(agora);
    assertThat(semBloqueio.getTentativasIncorretas()).isEqualTo(2);
  }

  @Test
  void devePermitirDefinirIdentificadorEId() {
    TentativaAutenticacaoWeb tentativa = new TentativaAutenticacaoWeb();
    java.util.UUID id = java.util.UUID.randomUUID();
    OffsetDateTime agora = OffsetDateTime.now();

    tentativa.setId(id);
    tentativa.setIdentificador("outra-origem");
    tentativa.setAtualizadoEm(agora);

    assertThat(tentativa.getId()).isEqualTo(id);
    assertThat(tentativa.getIdentificador()).isEqualTo("outra-origem");
    assertThat(tentativa.getAtualizadoEm()).isEqualTo(agora);
  }
}
