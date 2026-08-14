package com.escolademusica.relatorios.config;

import com.escolademusica.relatorios.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica as requisicoes do portal do professor ({@code /api/v1/professor/**}) a partir do Bearer
 * JWT com {@code role=PROFESSOR} (spec 002, T011).
 *
 * <p><b>Sessao deslizante (FR-005):</b> a cada requisicao autenticada com sucesso, um token novo —
 * com a janela de inatividade reiniciada — e devolvido no header de resposta {@value
 * #HEADER_TOKEN_RENOVADO}. O frontend ({@code useSessaoProfessor}) substitui o token armazenado por
 * esse valor, de modo que a sessao so expira apos inatividade real, sem endpoint de refresh
 * dedicado.
 *
 * <p>Tokens ausentes, invalidos, expirados ou de administrador (sem a claim {@code role=PROFESSOR})
 * simplesmente nao autenticam a requisicao — o {@code authenticationEntryPoint} da cadeia responde
 * 401, mesmo tratamento adotado pela cadeia administrativa.
 */
public class ProfessorJwtAuthenticationFilter extends OncePerRequestFilter {

  /** Header de resposta que carrega o token renovado da sessao deslizante. */
  public static final String HEADER_TOKEN_RENOVADO = "X-Refreshed-Token";

  /** Authority concedida ao professor autenticado no portal. */
  public static final String AUTHORITY_PROFESSOR = "ROLE_PROFESSOR";

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;

  public ProfessorJwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    extrairToken(request)
        .flatMap(jwtService::validarEExtrairProfessorId)
        .ifPresent(professorId -> autenticar(professorId, response));
    filterChain.doFilter(request, response);
  }

  private void autenticar(UUID professorId, HttpServletResponse response) {
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            professorId, null, List.of(new SimpleGrantedAuthority(AUTHORITY_PROFESSOR)));
    SecurityContextHolder.getContext().setAuthentication(authentication);

    // Definido antes de a resposta ser escrita pelo controller (headers so podem ser adicionados
    // enquanto a resposta nao estiver comprometida).
    response.setHeader(HEADER_TOKEN_RENOVADO, jwtService.gerarTokenProfessor(professorId).token());
  }

  private Optional<String> extrairToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header == null
        || !header.startsWith(BEARER_PREFIX)
        || header.length() == BEARER_PREFIX.length()) {
      return Optional.empty();
    }
    return Optional.of(header.substring(BEARER_PREFIX.length()));
  }
}
