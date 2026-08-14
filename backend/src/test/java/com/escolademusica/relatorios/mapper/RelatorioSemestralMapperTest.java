package com.escolademusica.relatorios.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.dto.RelatorioSemestralLlmPayloadDto;
import com.escolademusica.relatorios.dto.RelatorioSemestralPdfConteudoDto;
import com.escolademusica.relatorios.dto.RelatorioSemestralResponseDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RelatorioSemestralMapperTest {

  private RelatorioSemestralMapper mapper;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mapper = new RelatorioSemestralMapper(objectMapper);
  }

  private JsonNode node(String texto) {
    return objectMapper.createObjectNode().put("campo", texto);
  }

  private RelatorioSemestralLlmPayloadDto payloadCompleto() {
    return new RelatorioSemestralLlmPayloadDto(
        node("gerais"),
        node("frequencia"),
        node("tecnica"),
        node("musicalidade"),
        node("leitura"),
        node("pontos"),
        node("estrategias"),
        node("familiar"),
        node("planejamento"),
        "parecer final");
  }

  @Test
  void lerPayloadLlmDeveDesserializarJsonValido() throws Exception {
    String json = objectMapper.writeValueAsString(payloadCompleto());

    RelatorioSemestralLlmPayloadDto resultado = mapper.lerPayloadLlm(json);

    assertThat(resultado.parecerFinal()).isEqualTo("parecer final");
  }

  @Test
  void lerPayloadLlmDeveLancarQuandoJsonInvalido() {
    assertThatThrownBy(() -> mapper.lerPayloadLlm("{invalido"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void aplicarPayloadDevePreencherCamposDaEntidade() {
    RelatorioSemestral entidade = new RelatorioSemestral();

    mapper.aplicarPayload(entidade, payloadCompleto());

    assertThat(entidade.getParecerFinal()).isEqualTo("parecer final");
    assertThat(entidade.getInformacoesGerais()).contains("gerais");
    assertThat(entidade.getFrequenciaEEstudo()).contains("frequencia");
    assertThat(entidade.getTecnica()).contains("tecnica");
    assertThat(entidade.getMusicalidade()).contains("musicalidade");
    assertThat(entidade.getLeituraEMemorizacao()).contains("leitura");
    assertThat(entidade.getPontosDeAtencao()).contains("pontos");
    assertThat(entidade.getEstrategiasPedagogicas()).contains("estrategias");
    assertThat(entidade.getAcompanhamentoFamiliar()).contains("familiar");
    assertThat(entidade.getPlanejamentoProximoSemestre()).contains("planejamento");
  }

  @Test
  void aplicarPayloadDeveTratarSecoesNulasComoNullNode() {
    RelatorioSemestral entidade = new RelatorioSemestral();
    RelatorioSemestralLlmPayloadDto payload =
        new RelatorioSemestralLlmPayloadDto(
            null, null, null, null, null, null, null, null, null, null);

    mapper.aplicarPayload(entidade, payload);

    assertThat(entidade.getInformacoesGerais()).isEqualTo("null");
    assertThat(entidade.getParecerFinal()).isEqualTo("");
  }

  @Test
  void paraPayloadDeveMontarPayloadAPartirDaEntidade() {
    RelatorioSemestral entidade = new RelatorioSemestral();
    entidade.setInformacoesGerais("{\"campo\":\"gerais\"}");
    entidade.setParecerFinal("parecer");

    RelatorioSemestralLlmPayloadDto payload = mapper.paraPayload(entidade);

    assertThat(payload.informacoesGerais().get("campo").asText()).isEqualTo("gerais");
    assertThat(payload.parecerFinal()).isEqualTo("parecer");
  }

  @Test
  void paraPayloadDeveTratarCamposNulosOuEmBrancoComoNullNode() {
    RelatorioSemestral entidade = new RelatorioSemestral();
    entidade.setInformacoesGerais(null);
    entidade.setFrequenciaEEstudo("   ");

    RelatorioSemestralLlmPayloadDto payload = mapper.paraPayload(entidade);

    assertThat(payload.informacoesGerais()).isEqualTo(NullNode.getInstance());
    assertThat(payload.frequenciaEEstudo()).isEqualTo(NullNode.getInstance());
  }

  @Test
  void lerJsonDeveLancarQuandoJsonPersistidoInvalido() {
    RelatorioSemestral entidade = new RelatorioSemestral();
    entidade.setInformacoesGerais("{nao e json valido");

    assertThatThrownBy(() -> mapper.paraPayload(entidade))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void escreverJsonPayloadDeveSerializarPayload() {
    String json = mapper.escreverJsonPayload(payloadCompleto());

    assertThat(json).contains("parecer final");
  }

  @Test
  void paraResponseDtoDeveMontarDtoComDadosDaEntidade() {
    RelatorioSemestral entidade = new RelatorioSemestral();
    entidade.setId(UUID.randomUUID());
    entidade.setQuantidadeRelatoriosAulaConsiderados(5);
    entidade.setParecerFinal("parecer");
    entidade.setVersao(2);

    RelatorioSemestralResponseDto dto = mapper.paraResponseDto(entidade);

    assertThat(dto.relatorioId()).isEqualTo(entidade.getId());
    assertThat(dto.status()).isEqualTo(entidade.getStatus().name());
    assertThat(dto.quantidadeRelatoriosAulaConsiderados()).isEqualTo(5);
    assertThat(dto.parecerFinal()).isEqualTo("parecer");
    assertThat(dto.versao()).isEqualTo(2);
  }

  @Test
  void paraConteudoPdfDeveMontarConteudoComDadosDaEntidadeEParametros() {
    RelatorioSemestral entidade = new RelatorioSemestral();
    entidade.setPeriodoInicio(LocalDate.of(2026, 1, 1));
    entidade.setPeriodoFim(LocalDate.of(2026, 6, 30));
    entidade.setQuantidadeRelatoriosAulaConsiderados(3);
    entidade.setParecerFinal("parecer");

    RelatorioSemestralPdfConteudoDto dto =
        mapper.paraConteudoPdf(entidade, "Aluno Teste", "Professor Teste", "1º semestre 2026");

    assertThat(dto.nomeAluno()).isEqualTo("Aluno Teste");
    assertThat(dto.nomeProfessor()).isEqualTo("Professor Teste");
    assertThat(dto.rotuloPeriodo()).isEqualTo("1º semestre 2026");
    assertThat(dto.periodoInicio()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(dto.periodoFim()).isEqualTo(LocalDate.of(2026, 6, 30));
    assertThat(dto.quantidadeRelatoriosAulaConsiderados()).isEqualTo(3);
    assertThat(dto.parecerFinal()).isEqualTo("parecer");
  }
}
