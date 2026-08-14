package com.escolademusica.relatorios.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.VinculoTelegram;
import com.escolademusica.relatorios.domain.exception.AlunoNaoAssociadoException;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.AlunoResumoDto;
import com.escolademusica.relatorios.dto.HistoricoAlunoResponseDto;
import com.escolademusica.relatorios.dto.ItemHistoricoDto;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.repository.VinculoTelegramRepository;
import com.escolademusica.relatorios.usecase.ConsultarHistoricoUseCase;
import com.escolademusica.relatorios.usecase.VincularContaTelegramUseCase;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TelegramWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class TelegramWebhookControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private VincularContaTelegramUseCase vincularContaTelegramUseCase;
  @MockBean private VinculoTelegramRepository vinculoTelegramRepository;
  @MockBean private ProfessorRepository professorRepository;
  @MockBean private ConsultarHistoricoUseCase consultarHistoricoUseCase;

  @Test
  void deveVincularComSucesso() throws Exception {
    UUID professorId = UUID.randomUUID();
    when(vincularContaTelegramUseCase.executar("123456789", "AB12CD34"))
        .thenReturn(
            new VincularContaTelegramUseCase.ResultadoVinculacao(professorId, "Maria Silva"));

    mockMvc
        .perform(
            post("/internal/v1/telegram/vinculacao")
                .contentType("application/json")
                .content("{\"telegramUserId\":\"123456789\",\"codigoVinculacao\":\"AB12CD34\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nomeProfessor").value("Maria Silva"));
  }

  @Test
  void deveRetornar404QuandoCodigoInvalido() throws Exception {
    when(vincularContaTelegramUseCase.executar("123456789", "INVALIDO"))
        .thenThrow(new RecursoNaoEncontradoException("Codigo invalido"));

    mockMvc
        .perform(
            post("/internal/v1/telegram/vinculacao")
                .contentType("application/json")
                .content("{\"telegramUserId\":\"123456789\",\"codigoVinculacao\":\"INVALIDO\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void deveConsultarVinculoExistente() throws Exception {
    UUID professorId = UUID.randomUUID();
    VinculoTelegram vinculo = new VinculoTelegram();
    vinculo.setProfessorId(professorId);
    vinculo.setTelegramUserId("123456789");

    Professor professor = new Professor();
    professor.setId(professorId);
    professor.setNome("Maria Silva");

    when(vinculoTelegramRepository.findByTelegramUserId("123456789"))
        .thenReturn(Optional.of(vinculo));
    when(professorRepository.findById(professorId)).thenReturn(Optional.of(professor));

    mockMvc
        .perform(get("/internal/v1/telegram/123456789/professor"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nomeProfessor").value("Maria Silva"));
  }

  @Test
  void deveRetornar404QuandoNaoVinculado() throws Exception {
    when(vinculoTelegramRepository.findByTelegramUserId("999")).thenReturn(Optional.empty());

    mockMvc.perform(get("/internal/v1/telegram/999/professor")).andExpect(status().isNotFound());
  }

  @Test
  void deveRetornarHistoricoDoAlunoAssociado() throws Exception {
    UUID professorId = UUID.randomUUID();
    UUID alunoId = UUID.randomUUID();
    HistoricoAlunoResponseDto historico =
        new HistoricoAlunoResponseDto(
            new AlunoResumoDto(alunoId, "Joao Pedro"),
            List.of(
                ItemHistoricoDto.deAula(
                    UUID.randomUUID(),
                    LocalDate.of(2026, 3, 10),
                    "APROVADO",
                    "http://pdf/aula.pdf",
                    null)));

    when(consultarHistoricoUseCase.executar(alunoId, professorId)).thenReturn(historico);

    mockMvc
        .perform(
            get("/internal/v1/professores/" + professorId + "/alunos/" + alunoId + "/historico"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aluno.nome").value("Joao Pedro"));
  }

  @Test
  void deveRetornar403QuandoAlunoNaoAssociadoAoProfessor() throws Exception {
    UUID professorId = UUID.randomUUID();
    UUID alunoId = UUID.randomUUID();

    when(consultarHistoricoUseCase.executar(alunoId, professorId))
        .thenThrow(new AlunoNaoAssociadoException("Aluno nao associado a este professor"));

    mockMvc
        .perform(
            get("/internal/v1/professores/" + professorId + "/alunos/" + alunoId + "/historico"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.codigo").value("ALUNO_NAO_ASSOCIADO"));
  }
}
