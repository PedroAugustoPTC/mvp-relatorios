package com.escolademusica.relatorios.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class N8nServiceTokenFilterTest {

  @AfterEach
  void limparContextoSeguranca() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void deveAutenticarQuandoTokenCorreto() throws Exception {
    N8nServiceTokenFilter filtro = new N8nServiceTokenFilter("token-secreto");
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    when(request.getHeader("X-N8N-Service-Token")).thenReturn("token-secreto");

    filtro.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("n8n");
  }

  @Test
  void deveRejeitarQuandoTokenAusente() throws Exception {
    N8nServiceTokenFilter filtro = new N8nServiceTokenFilter("token-secreto");
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    StringWriter escrita = new StringWriter();
    when(request.getHeader("X-N8N-Service-Token")).thenReturn(null);
    when(response.getWriter()).thenReturn(new PrintWriter(escrita));

    filtro.doFilterInternal(request, response, filterChain);

    verify(filterChain, never()).doFilter(request, response);
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    assertThat(escrita.toString()).contains("NAO_AUTENTICADO");
  }

  @Test
  void deveRejeitarQuandoTokenDivergente() throws Exception {
    N8nServiceTokenFilter filtro = new N8nServiceTokenFilter("token-secreto");
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    when(request.getHeader("X-N8N-Service-Token")).thenReturn("token-errado");
    when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

    filtro.doFilterInternal(request, response, filterChain);

    verify(filterChain, never()).doFilter(request, response);
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  void deveRejeitarQuandoTokenEsperadoNuloOuEmBranco() throws Exception {
    N8nServiceTokenFilter filtro = new N8nServiceTokenFilter("   ");
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain filterChain = mock(FilterChain.class);
    when(request.getHeader("X-N8N-Service-Token")).thenReturn("qualquer-coisa");
    when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

    filtro.doFilterInternal(request, response, filterChain);

    verify(filterChain, never()).doFilter(request, response);
  }
}
