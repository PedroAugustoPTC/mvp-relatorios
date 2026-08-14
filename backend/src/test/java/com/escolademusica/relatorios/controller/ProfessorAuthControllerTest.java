package com.escolademusica.relatorios.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.domain.exception.AutenticacaoBloqueadaException;
import com.escolademusica.relatorios.domain.exception.CodigoVinculacaoInvalidoException;
import com.escolademusica.relatorios.usecase.AutenticarProfessorPortalUseCase;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de fatia de {@link ProfessorAuthController} (T056): 200 com token, 401 para codigo
 * invalido e 429 para bloqueio temporario (SC-009).
 */
@WebMvcTest(controllers = ProfessorAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfessorAuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AutenticarProfessorPortalUseCase autenticarProfessorPortalUseCase;

  private static final String CORPO = "{\"codigoVinculacao\": \"ABC123\"}";

  @Test
  void deveDevolverTokenEProfessorQuandoOCodigoEValido() throws Exception {
    UUID professorId = UUID.randomUUID();
    when(autenticarProfessorPortalUseCase.autenticar(any(), eq("ABC123")))
        .thenReturn(
            new AutenticarProfessorPortalUseCase.Resultado(
                "jwt-do-professor", OffsetDateTime.now().plusHours(12), professorId, "Ana Souza"));

    mockMvc
        .perform(
            post("/api/v1/professor/auth/vincular").contentType("application/json").content(CORPO))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-do-professor"))
        .andExpect(jsonPath("$.professor.id").value(professorId.toString()))
        .andExpect(jsonPath("$.professor.nome").value("Ana Souza"));
  }

  @Test
  void deveResponder401ComCodigoInvalidoSemRevelarSeOCodigoExiste() throws Exception {
    when(autenticarProfessorPortalUseCase.autenticar(any(), any()))
        .thenThrow(
            new CodigoVinculacaoInvalidoException("Codigo de vinculacao invalido ou expirado"));

    mockMvc
        .perform(
            post("/api/v1/professor/auth/vincular").contentType("application/json").content(CORPO))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"))
        .andExpect(jsonPath("$.token").doesNotExist());
  }

  @Test
  void deveResponder429ComTempoDeEsperaQuandoBloqueado() throws Exception {
    when(autenticarProfessorPortalUseCase.autenticar(any(), any()))
        .thenThrow(
            new AutenticacaoBloqueadaException(
                "Muitas tentativas incorretas. Tente novamente em alguns minutos.", 900));

    mockMvc
        .perform(
            post("/api/v1/professor/auth/vincular").contentType("application/json").content(CORPO))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.codigo").value("BLOQUEADO_TEMPORARIAMENTE"))
        .andExpect(jsonPath("$.retryAfterSeconds").value(900))
        .andExpect(header().string("Retry-After", "900"));
  }

  @Test
  void deveResponder400QuandoOCodigoNaoEInformado() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/professor/auth/vincular")
                .contentType("application/json")
                .content("{\"codigoVinculacao\": \"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
  }
}
