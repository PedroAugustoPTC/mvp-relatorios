package com.escolademusica.relatorios.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class RelatorioAulaLlmPayloadDtoTest {

  @Test
  void construtorCompactoDeveNormalizarTodosOsCamposNulos() {
    RelatorioAulaLlmPayloadDto dto =
        new RelatorioAulaLlmPayloadDto(null, null, null, null, null, null);

    assertThat(dto.conteudosTrabalhados()).isEmpty();
    assertThat(dto.evolucao()).isEmpty();
    assertThat(dto.dificuldades()).isEmpty();
    assertThat(dto.atividadesPropostas()).isEmpty();
    assertThat(dto.observacoes()).isEmpty();
    assertThat(dto.perguntasPendentes()).isEmpty();
  }

  @Test
  void construtorCompactoDevePreservarValoresInformados() {
    RelatorioAulaLlmPayloadDto dto =
        new RelatorioAulaLlmPayloadDto(
            List.of("c1"), "evo", List.of("d1"), List.of("a1"), "obs", List.of("p1"));

    assertThat(dto.conteudosTrabalhados()).containsExactly("c1");
    assertThat(dto.evolucao()).isEqualTo("evo");
    assertThat(dto.dificuldades()).containsExactly("d1");
    assertThat(dto.atividadesPropostas()).containsExactly("a1");
    assertThat(dto.observacoes()).isEqualTo("obs");
    assertThat(dto.perguntasPendentes()).containsExactly("p1");
  }
}
