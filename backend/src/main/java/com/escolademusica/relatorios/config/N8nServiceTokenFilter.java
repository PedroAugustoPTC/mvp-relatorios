package com.escolademusica.relatorios.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica requisicoes do workflow n8n aos endpoints {@code /internal/v1/**} comparando o header
 * {@code X-N8N-Service-Token} com o segredo configurado em {@code security.n8n-service-token}
 * (variavel de ambiente N8N_SERVICE_TOKEN, ver plan.md "Constraints"). Header ausente ou valor
 * divergente resulta em 401, sem autenticar a requisicao.
 */
@Component
public class N8nServiceTokenFilter extends OncePerRequestFilter {

  private static final String HEADER_NAME = "X-N8N-Service-Token";

  private final String tokenEsperado;

  public N8nServiceTokenFilter(@Value("${security.n8n-service-token}") String tokenEsperado) {
    this.tokenEsperado = tokenEsperado;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String tokenRecebido = request.getHeader(HEADER_NAME);

    if (tokenEsperado != null && !tokenEsperado.isBlank() && tokenEsperado.equals(tokenRecebido)) {
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(
              "n8n", null, List.of(new SimpleGrantedAuthority("ROLE_N8N_SERVICE")));
      SecurityContextHolder.getContext().setAuthentication(authentication);
      filterChain.doFilter(request, response);
      return;
    }

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response
        .getWriter()
        .write(
            "{\"codigo\":\"NAO_AUTENTICADO\",\"mensagem\":\"Token de servico n8n ausente ou invalido\"}");
  }
}
