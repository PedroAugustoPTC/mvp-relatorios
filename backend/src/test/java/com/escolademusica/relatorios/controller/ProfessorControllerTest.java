package com.escolademusica.relatorios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.exception.ConflitoException;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.repository.VinculoTelegramRepository;
import com.escolademusica.relatorios.usecase.CadastrarProfessorUseCase;
import com.escolademusica.relatorios.usecase.ReemitirCodigoVinculacaoUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProfessorController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfessorControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private CadastrarProfessorUseCase cadastrarProfessorUseCase;
  @MockBean private ReemitirCodigoVinculacaoUseCase reemitirCodigoVinculacaoUseCase;
  @MockBean private ProfessorRepository professorRepository;
  @MockBean private VinculoTelegramRepository vinculoTelegramRepository;

  @Test
  void deveCadastrarProfessorComSucesso() throws Exception {
    Professor professor = new Professor();
    professor.setId(UUID.randomUUID());
    professor.setNome("Maria Silva");
    professor.setEmail("maria@escola.com");
    professor.setCodigoVinculacao("AB12CD34");
    professor.setCodigoVinculacaoExpiraEm(OffsetDateTime.now().plusHours(24));

    when(cadastrarProfessorUseCase.executar("Maria Silva", "maria@escola.com"))
        .thenReturn(professor);

    mockMvc
        .perform(
            post("/api/v1/professores")
                .contentType("application/json")
                .content("{\"nome\":\"Maria Silva\",\"email\":\"maria@escola.com\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.codigoVinculacao").value("AB12CD34"));
  }

  @Test
  void deveRetornar409QuandoEmailDuplicado() throws Exception {
    when(cadastrarProfessorUseCase.executar(any(), any()))
        .thenThrow(new ConflitoException("PROFESSOR_EMAIL_DUPLICADO", "Ja cadastrado"));

    mockMvc
        .perform(
            post("/api/v1/professores")
                .contentType("application/json")
                .content("{\"nome\":\"Maria Silva\",\"email\":\"maria@escola.com\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("PROFESSOR_EMAIL_DUPLICADO"));
  }

  @Test
  void deveListarProfessoresComFlagDeVinculo() throws Exception {
    Professor professor = new Professor();
    professor.setId(UUID.randomUUID());
    professor.setNome("Maria Silva");
    professor.setEmail("maria@escola.com");

    when(professorRepository.findAll()).thenReturn(List.of(professor));
    when(vinculoTelegramRepository.findByProfessorId(professor.getId()))
        .thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/v1/professores"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].vinculadoTelegram").value(false));
  }
}
