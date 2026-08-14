package com.escolademusica.relatorios.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.VinculoTelegram;
import com.escolademusica.relatorios.domain.exception.ConflitoException;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.repository.VinculoTelegramRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VincularContaTelegramUseCaseTest {

  @Mock private ProfessorRepository professorRepository;
  @Mock private VinculoTelegramRepository vinculoTelegramRepository;

  private VincularContaTelegramUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new VincularContaTelegramUseCase(professorRepository, vinculoTelegramRepository);
  }

  private Professor professorComCodigoValido() {
    Professor professor = new Professor();
    professor.setId(UUID.randomUUID());
    professor.setNome("Maria Silva");
    professor.setCodigoVinculacao("AB12CD34");
    professor.setCodigoVinculacaoExpiraEm(OffsetDateTime.now().plusHours(1));
    return professor;
  }

  @Test
  void deveVincularEInvalidarCodigoAposUso() {
    Professor professor = professorComCodigoValido();
    when(professorRepository.findByCodigoVinculacao("AB12CD34")).thenReturn(Optional.of(professor));
    when(vinculoTelegramRepository.findByTelegramUserId("999")).thenReturn(Optional.empty());
    when(vinculoTelegramRepository.findByProfessorId(professor.getId()))
        .thenReturn(Optional.empty());
    when(professorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    VincularContaTelegramUseCase.ResultadoVinculacao resultado =
        useCase.executar("999", "AB12CD34");

    assertThat(resultado.professorId()).isEqualTo(professor.getId());
    assertThat(resultado.nomeProfessor()).isEqualTo("Maria Silva");
    assertThat(professor.getCodigoVinculacao()).isNull();
    assertThat(professor.getCodigoVinculacaoExpiraEm()).isNull();
  }

  @Test
  void deveRejeitarCodigoExpirado() {
    Professor professor = professorComCodigoValido();
    professor.setCodigoVinculacaoExpiraEm(OffsetDateTime.now().minusMinutes(1));
    when(professorRepository.findByCodigoVinculacao("AB12CD34")).thenReturn(Optional.of(professor));

    assertThatThrownBy(() -> useCase.executar("999", "AB12CD34"))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void deveRejeitarCodigoInexistente() {
    when(professorRepository.findByCodigoVinculacao("INVALIDO")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executar("999", "INVALIDO"))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void deveReVincularSemCriarNovoVinculoQuandoTelegramJaVinculadoAoMesmoProfessor() {
    Professor professor = professorComCodigoValido();
    when(professorRepository.findByCodigoVinculacao("AB12CD34")).thenReturn(Optional.of(professor));

    VinculoTelegram vinculoDoMesmoProfessor = new VinculoTelegram();
    vinculoDoMesmoProfessor.setProfessorId(professor.getId());
    vinculoDoMesmoProfessor.setTelegramUserId("999");
    when(vinculoTelegramRepository.findByTelegramUserId("999"))
        .thenReturn(Optional.of(vinculoDoMesmoProfessor));
    when(vinculoTelegramRepository.findByProfessorId(professor.getId()))
        .thenReturn(Optional.of(vinculoDoMesmoProfessor));
    when(professorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    VincularContaTelegramUseCase.ResultadoVinculacao resultado =
        useCase.executar("999", "AB12CD34");

    assertThat(resultado.professorId()).isEqualTo(professor.getId());
    org.mockito.Mockito.verify(vinculoTelegramRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  void deveRejeitarProfessorJaVinculadoAOutraContaTelegram() {
    Professor professor = professorComCodigoValido();
    when(professorRepository.findByCodigoVinculacao("AB12CD34")).thenReturn(Optional.of(professor));
    when(vinculoTelegramRepository.findByTelegramUserId("999")).thenReturn(Optional.empty());

    VinculoTelegram vinculoComOutroTelegram = new VinculoTelegram();
    vinculoComOutroTelegram.setProfessorId(professor.getId());
    vinculoComOutroTelegram.setTelegramUserId("111");
    when(vinculoTelegramRepository.findByProfessorId(professor.getId()))
        .thenReturn(Optional.of(vinculoComOutroTelegram));

    assertThatThrownBy(() -> useCase.executar("999", "AB12CD34"))
        .isInstanceOf(ConflitoException.class);
  }

  @Test
  void deveRejeitarTelegramJaVinculadoAOutroProfessor() {
    Professor professor = professorComCodigoValido();
    when(professorRepository.findByCodigoVinculacao("AB12CD34")).thenReturn(Optional.of(professor));

    VinculoTelegram vinculoDeOutroProfessor = new VinculoTelegram();
    vinculoDeOutroProfessor.setProfessorId(UUID.randomUUID());
    vinculoDeOutroProfessor.setTelegramUserId("999");
    when(vinculoTelegramRepository.findByTelegramUserId("999"))
        .thenReturn(Optional.of(vinculoDeOutroProfessor));

    assertThatThrownBy(() -> useCase.executar("999", "AB12CD34"))
        .isInstanceOf(ConflitoException.class);
  }
}
