package com.escolademusica.relatorios.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.escolademusica.relatorios.service.JwtService;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Testes do filtro de autenticacao do portal do professor (T017): contexto de seguranca populado,
 * header {@code X-Refreshed-Token} da sessao deslizante (FR-005) e recusa de tokens que nao sejam
 * de professor.
 */
class ProfessorJwtAuthenticationFilterTest {

  private JwtService jwtService;
  private ProfessorJwtAuthenticationFilter filtro;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private FilterChain chain;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService("segredo-de-teste-suficientemente-longo");
    filtro = new ProfessorJwtAuthenticationFilter(jwtService);
    request = new MockHttpServletRequest("GET", "/api/v1/professor/me");
    response = new MockHttpServletResponse();
    chain = new MockFilterChain();
  }

  @AfterEach
  void limparContexto() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void deveAutenticarERenovarOTokenQuandoBearerDeProfessorEValido() throws Exception {
    UUID professorId = UUID.randomUUID();
    String token = jwtService.gerarTokenProfessor(professorId).token();
    request.addHeader("Authorization", "Bearer " + token);

    filtro.doFilter(request, response, chain);

    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    assertThat(autenticacao).isNotNull();
    assertThat(autenticacao.getPrincipal()).isEqualTo(professorId);
    assertThat(autenticacao.getAuthorities())
        .extracting(Object::toString)
        .containsExactly(ProfessorJwtAuthenticationFilter.AUTHORITY_PROFESSOR);

    String tokenRenovado =
        response.getHeader(ProfessorJwtAuthenticationFilter.HEADER_TOKEN_RENOVADO);
    assertThat(tokenRenovado).isNotBlank();
    assertThat(jwtService.validarEExtrairProfessorId(tokenRenovado)).contains(professorId);
  }

  @Test
  void naoDeveAutenticarNemRenovarQuandoNaoHaHeaderAuthorization() throws Exception {
    filtro.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(response.getHeader(ProfessorJwtAuthenticationFilter.HEADER_TOKEN_RENOVADO)).isNull();
  }

  @Test
  void naoDeveAutenticarQuandoHeaderNaoUsaEsquemaBearerOuVemVazio() throws Exception {
    request.addHeader("Authorization", "Basic dXNlcjpzZW5oYQ==");

    filtro.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

    MockHttpServletRequest bearerVazio = new MockHttpServletRequest("GET", "/api/v1/professor/me");
    bearerVazio.addHeader("Authorization", "Bearer ");
    MockHttpServletResponse respostaVazia = new MockHttpServletResponse();

    filtro.doFilter(bearerVazio, respostaVazia, new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(respostaVazia.getHeader(ProfessorJwtAuthenticationFilter.HEADER_TOKEN_RENOVADO))
        .isNull();
  }

  @Test
  void naoDeveAutenticarComTokenInvalido() throws Exception {
    request.addHeader("Authorization", "Bearer token-invalido");

    filtro.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(response.getHeader(ProfessorJwtAuthenticationFilter.HEADER_TOKEN_RENOVADO)).isNull();
  }

  @Test
  void naoDeveAutenticarComTokenDeAdministrador() throws Exception {
    request.addHeader(
        "Authorization", "Bearer " + jwtService.gerarToken("admin@escola.com").token());

    filtro.doFilter(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(response.getHeader(ProfessorJwtAuthenticationFilter.HEADER_TOKEN_RENOVADO)).isNull();
  }
}
