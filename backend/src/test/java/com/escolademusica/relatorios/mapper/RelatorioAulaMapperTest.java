package com.escolademusica.relatorios.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.dto.EstruturarRelatorioResponseDto;
import com.escolademusica.relatorios.dto.RelatorioAulaLlmPayloadDto;
import com.escolademusica.relatorios.dto.RelatorioAulaPdfConteudoDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RelatorioAulaMapperTest {

  private RelatorioAulaMapper mapper;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mapper = new RelatorioAulaMapper(objectMapper);
  }

  private RelatorioAulaLlmPayloadDto payloadCompleto() {
    return new RelatorioAulaLlmPayloadDto(
        List.of("conteudo1", "conteudo2"),
        "evoluiu bem",
        List.of("dificuldade1"),
        List.of("atividade1"),
        "observacao",
        List.of("pergunta1"));
  }

  @Test
  void lerPayloadLlmDeveDesserializarJsonValido() throws Exception {
    String json = objectMapper.writeValueAsString(payloadCompleto());

    RelatorioAulaLlmPayloadDto resultado = mapper.lerPayloadLlm(json);

    assertThat(resultado.evolucao()).isEqualTo("evoluiu bem");
    assertThat(resultado.conteudosTrabalhados()).containsExactly("conteudo1", "conteudo2");
  }

  @Test
  void lerPayloadLlmDeveLancarQuandoJsonInvalido() {
    assertThatThrownBy(() -> mapper.lerPayloadLlm("{invalido"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void aplicarPayloadDevePreencherCamposDaEntidade() {
    RelatorioAula entidade = new RelatorioAula();

    mapper.aplicarPayload(entidade, payloadCompleto());

    assertThat(entidade.getEvolucao()).isEqualTo("evoluiu bem");
    assertThat(entidade.getObservacoes()).isEqualTo("observacao");
    assertThat(entidade.getConteudosTrabalhados()).contains("conteudo1");
    assertThat(entidade.getDificuldades()).contains("dificuldade1");
    assertThat(entidade.getAtividadesPropostas()).contains("atividade1");
  }

  @Test
  void aplicarPayloadDevePersistirAsPerguntasPendentesParaConsultaPosterior() {
    RelatorioAula entidade = new RelatorioAula();

    mapper.aplicarPayload(entidade, payloadCompleto());

    assertThat(mapper.perguntasPendentesDe(entidade))
        .containsExactlyElementsOf(payloadCompleto().perguntasPendentes());
  }

  @Test
  void perguntasPendentesDeDeveSerVaziaQuandoNadaFoiPersistido() {
    assertThat(mapper.perguntasPendentesDe(new RelatorioAula())).isEmpty();
  }

  @Test
  void paraPayloadDeveMontarPayloadAPartirDaEntidade() throws Exception {
    RelatorioAula entidade = new RelatorioAula();
    entidade.setConteudosTrabalhados(objectMapper.writeValueAsString(List.of("c1")));
    entidade.setEvolucao("evo");
    entidade.setDificuldades(objectMapper.writeValueAsString(List.of("d1")));
    entidade.setAtividadesPropostas(objectMapper.writeValueAsString(List.of("a1")));
    entidade.setObservacoes("obs");

    RelatorioAulaLlmPayloadDto payload = mapper.paraPayload(entidade, List.of("pergunta?"));

    assertThat(payload.conteudosTrabalhados()).containsExactly("c1");
    assertThat(payload.dificuldades()).containsExactly("d1");
    assertThat(payload.atividadesPropostas()).containsExactly("a1");
    assertThat(payload.evolucao()).isEqualTo("evo");
    assertThat(payload.observacoes()).isEqualTo("obs");
    assertThat(payload.perguntasPendentes()).containsExactly("pergunta?");
  }

  @Test
  void paraPayloadDeveTratarCamposJsonNulosOuEmBrancoComoListaVazia() {
    RelatorioAula entidade = new RelatorioAula();
    entidade.setConteudosTrabalhados(null);
    entidade.setDificuldades("");
    entidade.setAtividadesPropostas("   ");

    RelatorioAulaLlmPayloadDto payload = mapper.paraPayload(entidade, null);

    assertThat(payload.conteudosTrabalhados()).isEmpty();
    assertThat(payload.dificuldades()).isEmpty();
    assertThat(payload.atividadesPropostas()).isEmpty();
  }

  @Test
  void lerListaDeveLancarQuandoJsonPersistidoInvalido() {
    RelatorioAula entidade = new RelatorioAula();
    entidade.setConteudosTrabalhados("{nao e uma lista");

    assertThatThrownBy(() -> mapper.paraPayload(entidade, List.of()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void escreverJsonPayloadDeveSerializarPayload() {
    String json = mapper.escreverJsonPayload(payloadCompleto());

    assertThat(json).contains("evoluiu bem");
  }

  @Test
  void paraResponseDtoDeveMontarDtoComPerguntasPendentesNulas() {
    RelatorioAula entidade = new RelatorioAula();
    entidade.setId(UUID.randomUUID());
    entidade.setEvolucao("evo");
    entidade.setVersao(2);

    EstruturarRelatorioResponseDto dto = mapper.paraResponseDto(entidade, null);

    assertThat(dto.relatorioId()).isEqualTo(entidade.getId());
    assertThat(dto.status()).isEqualTo(entidade.getStatus().name());
    assertThat(dto.perguntasPendentes()).isEmpty();
    assertThat(dto.versao()).isEqualTo(2);
  }

  @Test
  void paraResponseDtoDeveMontarDtoComPerguntasPendentesPreenchidas() {
    RelatorioAula entidade = new RelatorioAula();
    entidade.setId(UUID.randomUUID());

    EstruturarRelatorioResponseDto dto = mapper.paraResponseDto(entidade, List.of("p1"));

    assertThat(dto.perguntasPendentes()).containsExactly("p1");
  }

  @Test
  void paraConteudoPdfDeveMontarConteudoComDadosDaEntidadeEParametros() {
    RelatorioAula entidade = new RelatorioAula();
    entidade.setEvolucao("evo");
    entidade.setObservacoes("obs");

    RelatorioAulaPdfConteudoDto dto =
        mapper.paraConteudoPdf(
            entidade, "Aluno Teste", "Professor Teste", LocalDate.of(2026, 3, 1));

    assertThat(dto.nomeAluno()).isEqualTo("Aluno Teste");
    assertThat(dto.nomeProfessor()).isEqualTo("Professor Teste");
    assertThat(dto.dataAula()).isEqualTo(LocalDate.of(2026, 3, 1));
    assertThat(dto.evolucao()).isEqualTo("evo");
    assertThat(dto.observacoes()).isEqualTo("obs");
  }
}
