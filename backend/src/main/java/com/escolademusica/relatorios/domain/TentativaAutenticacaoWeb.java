package com.escolademusica.relatorios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Contador de tentativas de autenticacao por codigo de vinculacao no portal web, usado para o rate
 * limit de FR-004a (spec 002).
 *
 * <p>Indexada pelo {@code identificador} (hash da origem da requisicao — IP/dispositivo) e nao pelo
 * professor, porque o limite precisa ser avaliado antes de validar o codigo, quando ainda nao se
 * sabe a qual professor a tentativa se refere (o codigo pode nem existir).
 */
@Entity
@Table(name = "tentativa_autenticacao_web")
public class TentativaAutenticacaoWeb {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "identificador", nullable = false, unique = true, length = 128)
  private String identificador;

  @Column(name = "tentativas_incorretas", nullable = false)
  private int tentativasIncorretas;

  @Column(name = "bloqueado_ate")
  private OffsetDateTime bloqueadoAte;

  @Column(name = "atualizado_em", nullable = false)
  private OffsetDateTime atualizadoEm;

  public TentativaAutenticacaoWeb() {}

  public TentativaAutenticacaoWeb(String identificador, OffsetDateTime momento) {
    this.identificador = identificador;
    this.tentativasIncorretas = 0;
    this.atualizadoEm = momento;
  }

  /** Ha bloqueio vigente neste instante? */
  public boolean estaBloqueadoEm(OffsetDateTime momento) {
    return bloqueadoAte != null && momento.isBefore(bloqueadoAte);
  }

  /**
   * Registra uma tentativa incorreta e, ao atingir {@code maxTentativas}, inicia um bloqueio de
   * {@code duracaoBloqueioMinutos} a partir de {@code momento}.
   */
  public void registrarFalha(
      OffsetDateTime momento, int maxTentativas, int duracaoBloqueioMinutos) {
    this.tentativasIncorretas += 1;
    this.atualizadoEm = momento;
    if (this.tentativasIncorretas >= maxTentativas) {
      this.bloqueadoAte = momento.plusMinutes(duracaoBloqueioMinutos);
    }
  }

  /** Zera contador e bloqueio apos uma autenticacao bem-sucedida. */
  public void registrarSucesso(OffsetDateTime momento) {
    this.tentativasIncorretas = 0;
    this.bloqueadoAte = null;
    this.atualizadoEm = momento;
  }

  /**
   * Descarta um bloqueio ja expirado, reiniciando a contagem — sem isto, uma unica tentativa
   * incorreta apos o fim do bloqueio reativaria o bloqueio imediatamente (contador ainda no
   * limite).
   */
  public void limparBloqueioExpirado(OffsetDateTime momento) {
    if (bloqueadoAte != null && !momento.isBefore(bloqueadoAte)) {
      this.tentativasIncorretas = 0;
      this.bloqueadoAte = null;
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getIdentificador() {
    return identificador;
  }

  public void setIdentificador(String identificador) {
    this.identificador = identificador;
  }

  public int getTentativasIncorretas() {
    return tentativasIncorretas;
  }

  public void setTentativasIncorretas(int tentativasIncorretas) {
    this.tentativasIncorretas = tentativasIncorretas;
  }

  public OffsetDateTime getBloqueadoAte() {
    return bloqueadoAte;
  }

  public void setBloqueadoAte(OffsetDateTime bloqueadoAte) {
    this.bloqueadoAte = bloqueadoAte;
  }

  public OffsetDateTime getAtualizadoEm() {
    return atualizadoEm;
  }

  public void setAtualizadoEm(OffsetDateTime atualizadoEm) {
    this.atualizadoEm = atualizadoEm;
  }
}
