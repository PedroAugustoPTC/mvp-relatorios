package com.escolademusica.relatorios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RelatorioAulaTest {

  @Test
  void novoRelatorioComecaComoRascunho() {
    RelatorioAula relatorio = new RelatorioAula();

    assertThat(relatorio.getStatus()).isEqualTo(RelatorioAula.StatusRelatorioAula.RASCUNHO);
    assertThat(relatorio.getVersao()).isEqualTo(1);
  }

  @Test
  void marcarPendenteRevisaoAlteraStatus() {
    RelatorioAula relatorio = new RelatorioAula();

    relatorio.marcarPendenteRevisao();

    assertThat(relatorio.getStatus()).isEqualTo(RelatorioAula.StatusRelatorioAula.PENDENTE_REVISAO);
  }

  @Test
  void solicitarAlteracaoIncrementaVersao() {
    RelatorioAula relatorio = new RelatorioAula();

    relatorio.solicitarAlteracao();

    assertThat(relatorio.getVersao()).isEqualTo(2);
  }

  @Test
  void aprovarAPartirDePendenteRevisaoFuncionaEDefineAprovadoEm() {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.marcarPendenteRevisao();
    OffsetDateTime momento = OffsetDateTime.now();

    relatorio.aprovar(momento);

    assertThat(relatorio.getStatus()).isEqualTo(RelatorioAula.StatusRelatorioAula.APROVADO);
    assertThat(relatorio.getAprovadoEm()).isEqualTo(momento);
  }

  @Test
  void aprovarForaDePendenteRevisaoLancaExcecao() {
    RelatorioAula relatorio = new RelatorioAula();

    assertThatThrownBy(() -> relatorio.aprovar(OffsetDateTime.now()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void deveExporTodosOsGettersESetters() {
    RelatorioAula relatorio = new RelatorioAula();
    UUID id = UUID.randomUUID();
    UUID aulaId = UUID.randomUUID();
    UUID professorId = UUID.randomUUID();
    UUID alunoId = UUID.randomUUID();
    OffsetDateTime agora = OffsetDateTime.now();

    relatorio.setId(id);
    relatorio.setAulaId(aulaId);
    relatorio.setProfessorId(professorId);
    relatorio.setAlunoId(alunoId);
    relatorio.setTranscricao("transcricao");
    relatorio.setConteudosTrabalhados("[]");
    relatorio.setEvolucao("evolucao");
    relatorio.setDificuldades("[]");
    relatorio.setAtividadesPropostas("[]");
    relatorio.setObservacoes("obs");
    relatorio.setVersao(3);
    relatorio.setStatus(RelatorioAula.StatusRelatorioAula.APROVADO);
    relatorio.setPdfUrl("/pdfs/a.pdf");
    relatorio.setCriadoEm(agora);
    relatorio.setAtualizadoEm(agora);
    relatorio.setAprovadoEm(agora);

    assertThat(relatorio.getId()).isEqualTo(id);
    assertThat(relatorio.getAulaId()).isEqualTo(aulaId);
    assertThat(relatorio.getProfessorId()).isEqualTo(professorId);
    assertThat(relatorio.getAlunoId()).isEqualTo(alunoId);
    assertThat(relatorio.getTranscricao()).isEqualTo("transcricao");
    assertThat(relatorio.getConteudosTrabalhados()).isEqualTo("[]");
    assertThat(relatorio.getEvolucao()).isEqualTo("evolucao");
    assertThat(relatorio.getDificuldades()).isEqualTo("[]");
    assertThat(relatorio.getAtividadesPropostas()).isEqualTo("[]");
    assertThat(relatorio.getObservacoes()).isEqualTo("obs");
    assertThat(relatorio.getVersao()).isEqualTo(3);
    assertThat(relatorio.getStatus()).isEqualTo(RelatorioAula.StatusRelatorioAula.APROVADO);
    assertThat(relatorio.getPdfUrl()).isEqualTo("/pdfs/a.pdf");
    assertThat(relatorio.getCriadoEm()).isEqualTo(agora);
    assertThat(relatorio.getAtualizadoEm()).isEqualTo(agora);
    assertThat(relatorio.getAprovadoEm()).isEqualTo(agora);
  }
}
