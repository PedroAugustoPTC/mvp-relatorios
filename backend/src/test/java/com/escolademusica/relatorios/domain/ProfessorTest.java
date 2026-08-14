package com.escolademusica.relatorios.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfessorTest {

  @Test
  void codigoValidoQuandoNaoExpiradoENaoNulo() {
    Professor professor = new Professor();
    professor.setCodigoVinculacao("AB12CD34");
    professor.setCodigoVinculacaoExpiraEm(OffsetDateTime.now().plusHours(1));

    assertThat(professor.codigoVinculacaoValidoEm(OffsetDateTime.now())).isTrue();
  }

  @Test
  void codigoInvalidoQuandoExpirado() {
    Professor professor = new Professor();
    professor.setCodigoVinculacao("AB12CD34");
    professor.setCodigoVinculacaoExpiraEm(OffsetDateTime.now().minusMinutes(1));

    assertThat(professor.codigoVinculacaoValidoEm(OffsetDateTime.now())).isFalse();
  }

  @Test
  void codigoInvalidoQuandoCodigoNulo() {
    Professor professor = new Professor();
    professor.setCodigoVinculacaoExpiraEm(OffsetDateTime.now().plusHours(1));

    assertThat(professor.codigoVinculacaoValidoEm(OffsetDateTime.now())).isFalse();
  }

  @Test
  void codigoInvalidoQuandoExpiracaoNula() {
    Professor professor = new Professor();
    professor.setCodigoVinculacao("AB12CD34");

    assertThat(professor.codigoVinculacaoValidoEm(OffsetDateTime.now())).isFalse();
  }

  @Test
  void deveExporTodosOsGettersESetters() {
    Professor professor = new Professor();
    UUID id = UUID.randomUUID();
    OffsetDateTime agora = OffsetDateTime.now();

    professor.setId(id);
    professor.setNome("Carlos");
    professor.setEmail("carlos@escola.com");
    professor.setAtivo(false);
    professor.setCodigoVinculacao("XYZ98765");
    professor.setCodigoVinculacaoExpiraEm(agora);
    professor.setCriadoEm(agora);
    professor.setAtualizadoEm(agora);

    assertThat(professor.getId()).isEqualTo(id);
    assertThat(professor.getNome()).isEqualTo("Carlos");
    assertThat(professor.getEmail()).isEqualTo("carlos@escola.com");
    assertThat(professor.isAtivo()).isFalse();
    assertThat(professor.getCodigoVinculacao()).isEqualTo("XYZ98765");
    assertThat(professor.getCodigoVinculacaoExpiraEm()).isEqualTo(agora);
    assertThat(professor.getCriadoEm()).isEqualTo(agora);
    assertThat(professor.getAtualizadoEm()).isEqualTo(agora);
  }
}
