package com.escolademusica.relatorios.config;

import com.escolademusica.relatorios.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Configuracao de seguranca com duas cadeias de filtro independentes, conforme plan.md
 * ("Constraints"):
 *
 * <ul>
 *   <li>{@code /api/v1/**} — interface administrativa web, autenticacao JWT de administrador.
 *   <li>{@code /internal/v1/**} — endpoints internos chamados pelo workflow do n8n, autenticados
 *       pelo header {@code X-N8N-Service-Token} comparado ao valor de {@code
 *       security.n8n-service-token} (env var N8N_SERVICE_TOKEN).
 * </ul>
 *
 * <p><b>T058:</b> {@link JwtAuthenticationFilter} agora valida assinatura e expiracao reais do JWT
 * (via {@link JwtService}) e extrai o e-mail do administrador (subject do token) como principal,
 * substituindo o esqueleto anterior que apenas checava a presenca de um bearer token opaco. O
 * endpoint de login que emite o token vive em {@code AuthController}/{@code
 * AutenticarAdminUseCase}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /** BCrypt para hash da senha do administrador (usado por {@code AutenticarAdminUseCase}). */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * {@link N8nServiceTokenFilter} e um {@code @Component}, o que faz o Spring Boot registra-lo
   * automaticamente como filtro de servlet GLOBAL (via {@code ServletContextInitializerBeans}), em
   * cima do uso explicito via {@code .addFilterBefore(...)} na cadeia {@code /internal/v1/**}
   * abaixo. Sem isto, o filtro passa a interceptar TODAS as requisicoes (incluindo {@code
   * /api/v1/**} e {@code /actuator/health}), exigindo X-N8N-Service-Token de qualquer chamada e
   * quebrando o login administrativo. Este bean desativa apenas o registro automatico global,
   * mantendo o filtro ativo (e corretamente restrito) dentro de {@link #internalFilterChain}.
   */
  @Bean
  public org.springframework.boot.web.servlet.FilterRegistrationBean<N8nServiceTokenFilter>
      n8nServiceTokenFilterRegistration(N8nServiceTokenFilter filter) {
    org.springframework.boot.web.servlet.FilterRegistrationBean<N8nServiceTokenFilter>
        registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  /** Cadeia de filtros para a interface administrativa web ({@code /api/v1/**}). */
  @Bean
  @Order(1)
  public SecurityFilterChain apiFilterChain(HttpSecurity http, JwtService jwtService)
      throws Exception {
    http.securityMatcher("/api/v1/**")
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtService),
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
                .class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/auth/login").permitAll().anyRequest().authenticated())
        .exceptionHandling(
            eh -> eh.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
    return http.build();
  }

  /** Cadeia de filtros para os endpoints internos chamados pelo n8n ({@code /internal/v1/**}). */
  @Bean
  @Order(2)
  public SecurityFilterChain internalFilterChain(
      HttpSecurity http, N8nServiceTokenFilter n8nServiceTokenFilter) throws Exception {
    http.securityMatcher("/internal/v1/**")
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(
            n8nServiceTokenFilter,
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
                .class)
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .exceptionHandling(
            eh -> eh.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
    return http.build();
  }

  /**
   * Cadeia "catch-all" para as demais rotas (ex.: {@code /actuator/health}, docs do
   * springdoc/swagger), liberadas sem autenticacao.
   */
  @Bean
  @Order(3)
  public SecurityFilterChain publicFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/actuator/health",
                        "/actuator/health/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .denyAll());
    return http.build();
  }

  /**
   * Filtro JWT (T058): valida assinatura e expiracao do bearer token via {@link JwtService} e, se
   * valido, autentica a requisicao usando o e-mail do administrador (subject do token) como
   * principal. Tokens ausentes/invalidos/expirados simplesmente nao autenticam a requisicao — o
   * {@code authenticationEntryPoint} da cadeia responde 401 nesse caso.
   */
  static class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    JwtAuthenticationFilter(JwtService jwtService) {
      this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
      String header = request.getHeader("Authorization");
      if (header != null
          && header.startsWith(BEARER_PREFIX)
          && header.length() > BEARER_PREFIX.length()) {
        String token = header.substring(BEARER_PREFIX.length());
        Optional<String> emailAdministrador = jwtService.validarEExtrairSubject(token);
        emailAdministrador.ifPresent(
            email -> {
              UsernamePasswordAuthenticationToken authentication =
                  new UsernamePasswordAuthenticationToken(
                      email, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
              SecurityContextHolder.getContext().setAuthentication(authentication);
            });
      }
      filterChain.doFilter(request, response);
    }
  }
}
