package com.escolademusica.relatorios.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

class RelatorioSemestralLlmPayloadDtoTest {

  @Test
  void construtorCompactoDeveNormalizarTodosOsCamposNulos() {
    RelatorioSemestralLlmPayloadDto dto =
        new RelatorioSemestralLlmPayloadDto(
            null, null, null, null, null, null, null, null, null, null);

    assertThat(dto.informacoesGerais()).isEqualTo(NullNode.getInstance());
    assertThat(dto.frequenciaEEstudo()).isEqualTo(NullNode.getInstance());
    assertThat(dto.tecnica()).isEqualTo(NullNode.getInstance());
    assertThat(dto.musicalidade()).isEqualTo(NullNode.getInstance());
    assertThat(dto.leituraEMemorizacao()).isEqualTo(NullNode.getInstance());
    assertThat(dto.pontosDeAtencao()).isEqualTo(NullNode.getInstance());
    assertThat(dto.estrategiasPedagogicas()).isEqualTo(NullNode.getInstance());
    assertThat(dto.acompanhamentoFamiliar()).isEqualTo(NullNode.getInstance());
    assertThat(dto.planejamentoProximoSemestre()).isEqualTo(NullNode.getInstance());
    assertThat(dto.parecerFinal()).isEmpty();
  }

  @Test
  void construtorCompactoDevePreservarValoresInformados() {
    TextNode node = TextNode.valueOf("valor");
    RelatorioSemestralLlmPayloadDto dto =
        new RelatorioSemestralLlmPayloadDto(
            node, node, node, node, node, node, node, node, node, "parecer");

    assertThat(dto.informacoesGerais()).isEqualTo(node);
    assertThat(dto.parecerFinal()).isEqualTo("parecer");
  }
}
