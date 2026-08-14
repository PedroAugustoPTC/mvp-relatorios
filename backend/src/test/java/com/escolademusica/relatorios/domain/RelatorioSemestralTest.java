package com.escolademusica.relatorios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RelatorioSemestralTest {

  @Test
  void novoRelatorioComecaComoPendenteRevisao() {
    RelatorioSemestral relatorio = new RelatorioSemestral();

    assertThat(relatorio.getStatus())
        .isEqualTo(RelatorioSemestral.StatusRelatorioSemestral.PENDENTE_REVISAO);
    assertThat(relatorio.getVersao()).isEqualTo(1);
  }

  @Test
  void solicitarAlteracaoIncrementaVersao() {
    RelatorioSemestral relatorio = new RelatorioSemestral();

    relatorio.solicitarAlteracao();

    assertThat(relatorio.getVersao()).isEqualTo(2);
  }

  @Test
  void aprovarAPartirDePendenteRevisaoFuncionaEDefineAprovadoEm() {
    RelatorioSemestral relatorio = new RelatorioSemestral();
    OffsetDateTime momento = OffsetDateTime.now();

    relatorio.aprovar(momento);

    assertThat(relatorio.getStatus())
        .isEqualTo(RelatorioSemestral.StatusRelatorioSemestral.APROVADO);
    assertThat(relatorio.getAprovadoEm()).isEqualTo(momento);
  }

  @Test
  void aprovarForaDePendenteRevisaoLancaExcecao() {
    RelatorioSemestral relatorio = new RelatorioSemestral();
    relatorio.aprovar(OffsetDateTime.now());

    assertThatThrownBy(() -> relatorio.aprovar(OffsetDateTime.now()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void deveExporTodosOsGettersESetters() {
    RelatorioSemestral relatorio = new RelatorioSemestral();
    UUID id = UUID.randomUUID();
    UUID alunoId = UUID.randomUUID();
    UUID professorId = UUID.randomUUID();
    OffsetDateTime agora = OffsetDateTime.now();
    LocalDate inicio = LocalDate.of(2026, 1, 1);
    LocalDate fim = LocalDate.of(2026, 6, 30);

    relatorio.setId(id);
    relatorio.setAlunoId(alunoId);
    relatorio.setProfessorId(professorId);
    relatorio.setPeriodoInicio(inicio);
    relatorio.setPeriodoFim(fim);
    relatorio.setQuantidadeRelatoriosAulaConsiderados(7);
    relatorio.setInformacoesGerais("{}");
    relatorio.setFrequenciaEEstudo("{}");
    relatorio.setTecnica("{}");
    relatorio.setMusicalidade("{}");
    relatorio.setLeituraEMemorizacao("{}");
    relatorio.setPontosDeAtencao("{}");
    relatorio.setEstrategiasPedagogicas("{}");
    relatorio.setAcompanhamentoFamiliar("{}");
    relatorio.setPlanejamentoProximoSemestre("{}");
    relatorio.setParecerFinal("parecer");
    relatorio.setVersao(2);
    relatorio.setStatus(RelatorioSemestral.StatusRelatorioSemestral.APROVADO);
    relatorio.setPdfUrl("/pdfs/s.pdf");
    relatorio.setCriadoEm(agora);
    relatorio.setAtualizadoEm(agora);
    relatorio.setAprovadoEm(agora);

    assertThat(relatorio.getId()).isEqualTo(id);
    assertThat(relatorio.getAlunoId()).isEqualTo(alunoId);
    assertThat(relatorio.getProfessorId()).isEqualTo(professorId);
    assertThat(relatorio.getPeriodoInicio()).isEqualTo(inicio);
    assertThat(relatorio.getPeriodoFim()).isEqualTo(fim);
    assertThat(relatorio.getQuantidadeRelatoriosAulaConsiderados()).isEqualTo(7);
    assertThat(relatorio.getInformacoesGerais()).isEqualTo("{}");
    assertThat(relatorio.getFrequenciaEEstudo()).isEqualTo("{}");
    assertThat(relatorio.getTecnica()).isEqualTo("{}");
    assertThat(relatorio.getMusicalidade()).isEqualTo("{}");
    assertThat(relatorio.getLeituraEMemorizacao()).isEqualTo("{}");
    assertThat(relatorio.getPontosDeAtencao()).isEqualTo("{}");
    assertThat(relatorio.getEstrategiasPedagogicas()).isEqualTo("{}");
    assertThat(relatorio.getAcompanhamentoFamiliar()).isEqualTo("{}");
    assertThat(relatorio.getPlanejamentoProximoSemestre()).isEqualTo("{}");
    assertThat(relatorio.getParecerFinal()).isEqualTo("parecer");
    assertThat(relatorio.getVersao()).isEqualTo(2);
    assertThat(relatorio.getStatus())
        .isEqualTo(RelatorioSemestral.StatusRelatorioSemestral.APROVADO);
    assertThat(relatorio.getPdfUrl()).isEqualTo("/pdfs/s.pdf");
    assertThat(relatorio.getCriadoEm()).isEqualTo(agora);
    assertThat(relatorio.getAtualizadoEm()).isEqualTo(agora);
    assertThat(relatorio.getAprovadoEm()).isEqualTo(agora);
  }
}
