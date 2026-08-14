package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.Aula;
import com.escolademusica.relatorios.domain.exception.AlunoNaoAssociadoException;
import com.escolademusica.relatorios.gateway.AudioNaoProcessavelException;
import com.escolademusica.relatorios.repository.AulaRepository;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.repository.RelatorioAulaRepository;
import com.escolademusica.relatorios.service.TranscricaoService;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrarAulaUseCaseTest {

  @Mock private ProfessorAlunoRepository professorAlunoRepository;
  @Mock private AulaRepository aulaRepository;
  @Mock private RelatorioAulaRepository relatorioAulaRepository;
  @Mock private TranscricaoService transcricaoService;

  private RegistrarAulaUseCase useCase;

  private final UUID professorId = UUID.randomUUID();
  private final UUID alunoId = UUID.randomUUID();
  private final byte[] audio = {1, 2, 3};

  @BeforeEach
  void setUp() {
    useCase =
        new RegistrarAulaUseCase(
            professorAlunoRepository, aulaRepository, relatorioAulaRepository, transcricaoService);
  }

  @Test
  void deveRegistrarAulaETranscreverQuandoAlunoAssociado() {
    when(professorAlunoRepository.existsByProfessorIdAndAlunoId(professorId, alunoId))
        .thenReturn(true);
    when(transcricaoService.transcrever(audio, "ogg")).thenReturn("transcricao da aula");
    when(aulaRepository.save(any()))
        .thenAnswer(
            invocation -> {
              Aula aula = invocation.getArgument(0);
              aula.setId(UUID.randomUUID());
              return aula;
            });

    RegistrarAulaUseCase.Resultado resultado =
        useCase.registrar(professorId, alunoId, LocalDate.now(), audio, "ogg");

    assertThat(resultado.aulaId()).isNotNull();
    assertThat(resultado.transcricao()).isEqualTo("transcricao da aula");
    verify(aulaRepository).save(any());
    verify(relatorioAulaRepository).save(any());
  }

  @Test
  void deveRejeitarAlunoNaoAssociadoAoProfessor() {
    when(professorAlunoRepository.existsByProfessorIdAndAlunoId(professorId, alunoId))
        .thenReturn(false);

    assertThatThrownBy(() -> useCase.registrar(professorId, alunoId, LocalDate.now(), audio, "ogg"))
        .isInstanceOf(AlunoNaoAssociadoException.class);

    verify(aulaRepository, never()).save(any());
    verify(relatorioAulaRepository, never()).save(any());
  }

  @Test
  void naoDevePersistirNadaQuandoAudioNaoProcessavel() {
    when(professorAlunoRepository.existsByProfessorIdAndAlunoId(professorId, alunoId))
        .thenReturn(true);
    when(transcricaoService.transcrever(audio, "ogg"))
        .thenThrow(new AudioNaoProcessavelException("audio corrompido"));

    assertThatThrownBy(() -> useCase.registrar(professorId, alunoId, LocalDate.now(), audio, "ogg"))
        .isInstanceOf(AudioNaoProcessavelException.class);

    verify(aulaRepository, never()).save(any());
    verify(relatorioAulaRepository, never()).save(any());
  }
}
