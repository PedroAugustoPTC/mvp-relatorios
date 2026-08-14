package com.escolademusica.relatorios.integration;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.controller.TelegramWebhookController;
import com.escolademusica.relatorios.domain.Professor;
import com.escolademusica.relatorios.domain.VinculoTelegram;
import com.escolademusica.relatorios.domain.exception.RecursoNaoEncontradoException;
import com.escolademusica.relatorios.repository.ProfessorRepository;
import com.escolademusica.relatorios.repository.VinculoTelegramRepository;
import com.escolademusica.relatorios.usecase.ConsultarHistoricoUseCase;
import com.escolademusica.relatorios.usecase.VincularContaTelegramUseCase;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * T100 — Testes de contrato (nao de negocio, ja coberto em {@code
 * controller.TelegramWebhookControllerTest}) para o fluxo de vinculacao Telegram <-> Professor de
 * contracts/api-n8n-integration.md: valida exatamente o shape JSON (nomes/tipos/aninhamento) de
 * requisicao e resposta dos endpoints {@code POST /internal/v1/telegram/vinculacao} e {@code GET
 * /internal/v1/telegram/{telegramUserId}/professor}.
 */
@WebMvcTest(controllers = TelegramWebhookController.class)
@AutoConfigureMockMvc(addFilters = false)
class TelegramVinculacaoContractTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private VincularContaTelegramUseCase vincularContaTelegramUseCase;
  @MockBean private VinculoTelegramRepository vinculoTelegramRepository;
  @MockBean private ProfessorRepository professorRepository;
  @MockBean private ConsultarHistoricoUseCase consultarHistoricoUseCase;

  @Test
  void requisicaoDeVinculacaoAceitaExatamenteTelegramUserIdECodigoVinculacao() throws Exception {
    UUID professorId = UUID.randomUUID();
    when(vincularContaTelegramUseCase.executar("123456789", "AB12CD"))
        .thenReturn(
            new VincularContaTelegramUseCase.ResultadoVinculacao(professorId, "Maria Silva"));

    mockMvc
        .perform(
            post("/internal/v1/telegram/vinculacao")
                .contentType("application/json")
                .content("{\"telegramUserId\":\"123456789\",\"codigoVinculacao\":\"AB12CD\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.professorId").value(professorId.toString()))
        .andExpect(jsonPath("$.nomeProfessor").value("Maria Silva"))
        // shape exato: apenas estes dois campos, nenhum campo extra (ex.: token, senha)
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void respostaDeVinculacao404NaoContemCorpoDeSucesso() throws Exception {
    when(vincularContaTelegramUseCase.executar("123456789", "EXPIRADO"))
        .thenThrow(new RecursoNaoEncontradoException("codigo invalido/expirado"));

    mockMvc
        .perform(
            post("/internal/v1/telegram/vinculacao")
                .contentType("application/json")
                .content("{\"telegramUserId\":\"123456789\",\"codigoVinculacao\":\"EXPIRADO\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").exists())
        .andExpect(jsonPath("$.mensagem").exists());
  }

  @Test
  void consultaDeVinculoRetornaMesmoShapeDaVinculacao() throws Exception {
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
        .andExpect(jsonPath("$.professorId").value(professorId.toString()))
        .andExpect(jsonPath("$.nomeProfessor").value("Maria Silva"))
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void consultaDeVinculoAindaNaoVinculadoRetorna404() throws Exception {
    when(vinculoTelegramRepository.findByTelegramUserId("999")).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/internal/v1/telegram/999/professor"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.codigo").exists());
  }
}
