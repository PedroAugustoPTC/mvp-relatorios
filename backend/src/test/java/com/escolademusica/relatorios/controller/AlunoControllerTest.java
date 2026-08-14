package com.escolademusica.relatorios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.domain.Aluno;
import com.escolademusica.relatorios.domain.exception.ConflitoException;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.dto.AlunoResumoDto;
import com.escolademusica.relatorios.dto.HistoricoAlunoResponseDto;
import com.escolademusica.relatorios.dto.ItemHistoricoDto;
import com.escolademusica.relatorios.repository.AlunoRepository;
import com.escolademusica.relatorios.repository.ProfessorAlunoRepository;
import com.escolademusica.relatorios.usecase.CadastrarAlunoUseCase;
import com.escolademusica.relatorios.usecase.ConsultarHistoricoUseCase;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AlunoController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlunoControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private CadastrarAlunoUseCase cadastrarAlunoUseCase;
  @MockBean private AlunoRepository alunoRepository;
  @MockBean private ProfessorAlunoRepository professorAlunoRepository;
  @MockBean private ConsultarHistoricoUseCase consultarHistoricoUseCase;

  @Test
  void deveCadastrarAlunoComSucesso() throws Exception {
    Aluno aluno = new Aluno();
    aluno.setId(UUID.randomUUID());
    aluno.setNome("Joao Pedro");
    aluno.setDataNascimento(LocalDate.of(1990, 1, 1));

    when(cadastrarAlunoUseCase.executar(anyString(), any(), anyString(), any(), anyList()))
        .thenReturn(aluno);

    mockMvc
        .perform(
            post("/api/v1/alunos")
                .contentType("application/json")
                .content(
                    "{\"nome\":\"Joao Pedro\",\"dataNascimento\":\"1990-01-01\",\"cpf\":\"00000000000\",\"professorIds\":[]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nome").value("Joao Pedro"));
  }

  @Test
  void deveRetornar409QuandoCpfDuplicado() throws Exception {
    when(cadastrarAlunoUseCase.executar(anyString(), any(), anyString(), any(), anyList()))
        .thenThrow(new ConflitoException("ALUNO_CPF_DUPLICADO", "Ja cadastrado"));

    mockMvc
        .perform(
            post("/api/v1/alunos")
                .contentType("application/json")
                .content(
                    "{\"nome\":\"Joao Pedro\",\"dataNascimento\":\"1990-01-01\",\"cpf\":\"00000000000\",\"professorIds\":[]}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.codigo").value("ALUNO_CPF_DUPLICADO"));
  }

  @Test
  void deveRetornar400QuandoMenorSemResponsavel() throws Exception {
    when(cadastrarAlunoUseCase.executar(anyString(), any(), anyString(), any(), anyList()))
        .thenThrow(new IllegalArgumentException("nomeResponsavel e obrigatorio"));

    mockMvc
        .perform(
            post("/api/v1/alunos")
                .contentType("application/json")
                .content(
                    "{\"nome\":\"Crianca\",\"dataNascimento\":\"2015-01-01\",\"cpf\":\"11111111111\",\"professorIds\":[]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void deveListarAlunos() throws Exception {
    when(alunoRepository.findAll()).thenReturn(List.of());

    mockMvc.perform(get("/api/v1/alunos")).andExpect(status().isOk());
  }

  @Test
  void deveRetornarHistoricoDoAluno() throws Exception {
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

    when(consultarHistoricoUseCase.executar(alunoId)).thenReturn(historico);

    mockMvc
        .perform(get("/api/v1/alunos/" + alunoId + "/historico"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aluno.nome").value("Joao Pedro"))
        .andExpect(jsonPath("$.relatorios[0].tipo").value("AULA"));
  }

  @Test
  void deveRetornar404QuandoAlunoDoHistoricoNaoExiste() throws Exception {
    UUID alunoId = UUID.randomUUID();
    when(consultarHistoricoUseCase.executar(alunoId))
        .thenThrow(new RecursoNaoEncontradoException("Aluno nao encontrado"));

    mockMvc
        .perform(get("/api/v1/alunos/" + alunoId + "/historico"))
        .andExpect(status().isNotFound());
  }
}
