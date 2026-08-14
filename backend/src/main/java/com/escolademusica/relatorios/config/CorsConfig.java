package com.escolademusica.relatorios.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS para as chamadas feitas diretamente pelo navegador a {@code /api/v1/**} — interface
 * administrativa e portal do professor (spec 002, research.md secao 4).
 *
 * <p>Ate a spec 001 o backend nao definia nenhuma configuracao de CORS; sem ela, o preflight {@code
 * OPTIONS} e bloqueado sempre que frontend e backend nao compartilham a mesma origem (o caso do
 * docker-compose, onde o frontend responde em 8080 e o backend em 8081).
 *
 * <p>A origem permitida e configuravel via {@code CORS_ALLOWED_ORIGIN} e nunca e {@code *}: como as
 * requisicoes carregam o header {@code Authorization}, uma origem curinga seria tanto recusada pelo
 * navegador (em conjunto com credenciais) quanto indesejavel do ponto de vista de seguranca.
 */
@Configuration
public class CorsConfig {

  private final List<String> origensPermitidas;

  public CorsConfig(
      @Value("${security.cors.allowed-origin:http://localhost:8080}") String origemPermitida) {
    this.origensPermitidas =
        java.util.Arrays.stream(origemPermitida.split(","))
            .map(String::trim)
            .filter(origem -> !origem.isEmpty())
            .toList();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuracao = new CorsConfiguration();
    configuracao.setAllowedOrigins(origensPermitidas);
    configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    // Sem isto o JavaScript do portal nao consegue ler o token renovado da sessao deslizante
    // (FR-005): headers de resposta em requisicoes cross-origin ficam ocultos por padrao.
    configuracao.setExposedHeaders(List.of(ProfessorJwtAuthenticationFilter.HEADER_TOKEN_RENOVADO));
    configuracao.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/v1/**", configuracao);
    return source;
  }
}
