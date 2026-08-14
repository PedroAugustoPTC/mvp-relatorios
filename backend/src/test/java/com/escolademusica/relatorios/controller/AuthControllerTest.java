package com.escolademusica.relatorios.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.escolademusica.relatorios.domain.exception.CredenciaisInvalidasException;
import com.escolademusica.relatorios.service.JwtService;
import com.escolademusica.relatorios.usecase.AutenticarAdminUseCase;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AutenticarAdminUseCase autenticarAdminUseCase;

  @Test
  void deveAutenticarComSucesso() throws Exception {
    when(autenticarAdminUseCase.executar("admin@escola.com", "senha123"))
        .thenReturn(new JwtService.TokenGerado("token-jwt", OffsetDateTime.now().plusHours(2)));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"admin@escola.com\",\"senha\":\"senha123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("token-jwt"));
  }

  @Test
  void deveRetornar401QuandoCredenciaisInvalidas() throws Exception {
    when(autenticarAdminUseCase.executar("admin@escola.com", "errada"))
        .thenThrow(new CredenciaisInvalidasException("E-mail ou senha invalidos."));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"admin@escola.com\",\"senha\":\"errada\"}"))
        .andExpect(status().isUnauthorized());
  }
}
