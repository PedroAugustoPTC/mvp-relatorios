package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.domain.Aula;
import com.escolademusica.relatorios.domain.RelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioAula.StatusRelatorioAula;
import com.escolademusica.relatorios.domain.RelatorioSemestral;
import com.escolademusica.relatorios.domain.RelatorioSemestral.StatusRelatorioSemestral;
import com.escolademusica.relatorios.domain.exception.AlunoNaoAssociadoException;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.HistoricoAlunoResponseDto;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.repository.AulaRepository;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.repository.RelatorioSemestralRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarHistoricoUseCaseTest {

  @Mock private AlunoRepository alunoRepository;
  @Mock private AulaRepository aulaRepository;
  @Mock private RelatorioAulaRepository relatorioAulaRepository;
  @Mock private RelatorioSemestralRepository relatorioSemestralRepository;
  @Mock private ProfessorAlunoRepository professorAlunoRepository;

  private ConsultarHistoricoUseCase useCase;

  private final UUID alunoId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    useCase =
        new ConsultarHistoricoUseCase(
            alunoRepository,
            aulaRepository,
            relatorioAulaRepository,
            relatorioSemestralRepository,
            professorAlunoRepository);
  }

  private Aluno alunoExistente() {
    Aluno aluno = new Aluno();
    aluno.setId(alunoId);
    aluno.setNome("Joao Pedro");
    return aluno;
  }

  @Test
  void deveRetornarHistoricoOrdenadoCronologicamenteApenasComAprovados() {
    when(alunoRepository.findById(alunoId)).thenReturn(Optional.of(alunoExistente()));

    UUID aulaId1 = UUID.randomUUID();
    UUID aulaId2 = UUID.randomUUID();

    RelatorioAula relatorioAprovado = new RelatorioAula();
    relatorioAprovado.setId(UUID.randomUUID());
    relatorioAprovado.setAulaId(aulaId1);
    relatorioAprovado.setStatus(StatusRelatorioAula.APROVADO);
    relatorioAprovado.setPdfUrl("http://pdf/aula1.pdf");
    relatorioAprovado.setAprovadoEm(OffsetDateTime.now());

    RelatorioAula relatorioPendente = new RelatorioAula();
    relatorioPendente.setId(UUID.randomUUID());
    relatorioPendente.setAulaId(aulaId2);
    relatorioPendente.setStatus(StatusRelatorioAula.PENDENTE_REVISAO);

    when(relatorioAulaRepository.findByAlunoIdOrderByCriadoEmAsc(alunoId))
        .thenReturn(List.of(relatorioAprovado, relatorioPendente));

    Aula aula1 = new Aula();
    aula1.setId(aulaId1);
    aula1.setDataAula(LocalDate.of(2026, 3, 10));
    when(aulaRepository.findById(aulaId1)).thenReturn(Optional.of(aula1));

    RelatorioSemestral semestralAprovado = new RelatorioSemestral();
    semestralAprovado.setId(UUID.randomUUID());
    semestralAprovado.setPeriodoInicio(LocalDate.of(2026, 1, 1));
    semestralAprovado.setPeriodoFim(LocalDate.of(2026, 6, 30));
    semestralAprovado.setStatus(StatusRelatorioSemestral.APROVADO);
    semestralAprovado.setPdfUrl("http://pdf/semestral.pdf");
    semestralAprovado.setAprovadoEm(OffsetDateTime.now());

    RelatorioSemestral semestralPendente = new RelatorioSemestral();
    semestralPendente.setId(UUID.randomUUID());
    semestralPendente.setStatus(StatusRelatorioSemestral.PENDENTE_REVISAO);

    when(relatorioSemestralRepository.findByAlunoIdOrderByPeriodoInicioAsc(alunoId))
        .thenReturn(List.of(semestralAprovado, semestralPendente));

    HistoricoAlunoResponseDto resultado = useCase.executar(alunoId);

    assertThat(resultado.aluno().id()).isEqualTo(alunoId);
    assertThat(resultado.relatorios()).hasSize(2);
    assertThat(resultado.relatorios().get(0).tipo()).isEqualTo("SEMESTRAL");
    assertThat(resultado.relatorios().get(1).tipo()).isEqualTo("AULA");
  }

  @Test
  void deveLancarNaoEncontradoQuandoAlunoNaoExiste() {
    when(alunoRepository.findById(alunoId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executar(alunoId))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void devePermitirAcessoAdminSemProfessorId() {
    when(alunoRepository.findById(alunoId)).thenReturn(Optional.of(alunoExistente()));
    when(relatorioAulaRepository.findByAlunoIdOrderByCriadoEmAsc(alunoId)).thenReturn(List.of());
    when(relatorioSemestralRepository.findByAlunoIdOrderByPeriodoInicioAsc(alunoId))
        .thenReturn(List.of());

    HistoricoAlunoResponseDto resultado = useCase.executar(alunoId);

    assertThat(resultado.relatorios()).isEmpty();
  }

  @Test
  void deveLancar403QuandoAlunoNaoAssociadoAoProfessor() {
    UUID professorId = UUID.randomUUID();
    when(alunoRepository.findById(alunoId)).thenReturn(Optional.of(alunoExistente()));
    when(professorAlunoRepository.existsByProfessorIdAndAlunoId(professorId, alunoId))
        .thenReturn(false);

    assertThatThrownBy(() -> useCase.executar(alunoId, professorId))
        .isInstanceOf(AlunoNaoAssociadoException.class);
  }

  @Test
  void devePermitirQuandoAlunoAssociadoAoProfessor() {
    UUID professorId = UUID.randomUUID();
    when(alunoRepository.findById(alunoId)).thenReturn(Optional.of(alunoExistente()));
    when(professorAlunoRepository.existsByProfessorIdAndAlunoId(professorId, alunoId))
        .thenReturn(true);
    when(relatorioAulaRepository.findByAlunoIdOrderByCriadoEmAsc(alunoId)).thenReturn(List.of());
    when(relatorioSemestralRepository.findByAlunoIdOrderByPeriodoInicioAsc(alunoId))
        .thenReturn(List.of());

    HistoricoAlunoResponseDto resultado = useCase.executar(alunoId, professorId);

    assertThat(resultado.relatorios()).isEmpty();
  }
}
