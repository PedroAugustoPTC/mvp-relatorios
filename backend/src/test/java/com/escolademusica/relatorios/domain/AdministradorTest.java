package com.escolademusica.relatorios.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdministradorTest {

  @Test
  void deveExporTodosOsGettersESetters() {
    Administrador administrador = new Administrador();
    UUID id = UUID.randomUUID();
    OffsetDateTime agora = OffsetDateTime.now();

    administrador.setId(id);
    administrador.setEmail("admin@escola.com");
    administrador.setSenhaHash("hash-bcrypt");
    administrador.setCriadoEm(agora);

    assertThat(administrador.getId()).isEqualTo(id);
    assertThat(administrador.getEmail()).isEqualTo("admin@escola.com");
    assertThat(administrador.getSenhaHash()).isEqualTo("hash-bcrypt");
    assertThat(administrador.getCriadoEm()).isEqualTo(agora);
  }
}
