package com.escolademusica.relatorios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.exception.AlunoNaoAssociadoException;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Testes do controle de acesso do portal (FR-018 aplicado ao canal web). E a barreira que impede um
 * professor de alcancar dados de outro, entao cada caminho e verificado diretamente aqui, e nao
 * apenas atraves dos controllers.
 */
class ControleAcessoProfessorServiceTest {

  private ProfessorAlunoRepository professorAlunoRepository;
  private RelatorioAulaRepository relatorioAulaRepository;
  private RelatorioSemestralRepository relatorioSemestralRepository;
  private ControleAcessoProfessorService service;

  private final UUID professorId = UUID.randomUUID();
  private final UUID outroProfessorId = UUID.randomUUID();
  private final UUID alunoId = UUID.randomUUID();
  private final UUID relatorioId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    professorAlunoRepository = Mockito.mock(ProfessorAlunoRepository.class);
    relatorioAulaRepository = Mockito.mock(RelatorioAulaRepository.class);
    relatorioSemestralRepository = Mockito.mock(RelatorioSemestralRepository.class);
    service =
        new ControleAcessoProfessorService(
            professorAlunoRepository, relatorioAulaRepository, relatorioSemestralRepository);
  }

  @Test
  void devePermitirAcessoAAlunoAssociado() {
    when(professorAlunoRepository.existsByProfessorIdAndAlunoId(professorId, alunoId))
        .thenReturn(true);

    assertThatCode(() -> service.exigirAlunoDoProfessor(professorId, alunoId))
        .doesNotThrowAnyException();
  }

  @Test
  void deveRecusarAlunoDeOutroProfessorCom403() {
    when(professorAlunoRepository.existsByProfessorIdAndAlunoId(professorId, alunoId))
        .thenReturn(false);

    assertThatThrownBy(() -> service.exigirAlunoDoProfessor(professorId, alunoId))
        .isInstanceOf(AlunoNaoAssociadoException.class);
  }

  @Test
  void deveDevolverORelatorioDeAulaDoProprioProfessor() {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setId(relatorioId);
    relatorio.setProfessorId(professorId);
    when(relatorioAulaRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));

    assertThat(service.exigirRelatorioAulaDoProfessor(professorId, relatorioId))
        .isSameAs(relatorio);
  }

  @Test
  void deveTratarRelatorioDeAulaDeOutroProfessorComoInexistente() {
    RelatorioAula relatorio = new RelatorioAula();
    relatorio.setId(relatorioId);
    relatorio.setProfessorId(outroProfessorId);
    when(relatorioAulaRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));

    // 404 e nao 403: responder 403 confirmaria a existencia do relatorio a quem nao deveria saber.
    assertThatThrownBy(() -> service.exigirRelatorioAulaDoProfessor(professorId, relatorioId))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void deveLancarQuandoORelatorioDeAulaNaoExiste() {
    when(relatorioAulaRepository.findById(relatorioId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.exigirRelatorioAulaDoProfessor(professorId, relatorioId))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void deveDevolverORelatorioSemestralDoProprioProfessor() {
    RelatorioSemestral relatorio = new RelatorioSemestral();
    relatorio.setId(relatorioId);
    relatorio.setProfessorId(professorId);
    when(relatorioSemestralRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));

    assertThat(service.exigirRelatorioSemestralDoProfessor(professorId, relatorioId))
        .isSameAs(relatorio);
  }

  @Test
  void deveTratarRelatorioSemestralDeOutroProfessorComoInexistente() {
    RelatorioSemestral relatorio = new RelatorioSemestral();
    relatorio.setId(relatorioId);
    relatorio.setProfessorId(outroProfessorId);
    when(relatorioSemestralRepository.findById(relatorioId)).thenReturn(Optional.of(relatorio));

    assertThatThrownBy(() -> service.exigirRelatorioSemestralDoProfessor(professorId, relatorioId))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void deveLancarQuandoORelatorioSemestralNaoExiste() {
    when(relatorioSemestralRepository.findById(relatorioId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.exigirRelatorioSemestralDoProfessor(professorId, relatorioId))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}
