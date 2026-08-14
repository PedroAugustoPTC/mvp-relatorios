package com.escolademusica.relatorios.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Testes da configuracao de CORS (T017, research.md secao 4): origem configuravel aplicada a {@code
 * /api/v1/**}, metodos/headers necessarios ao portal e exposicao do header da sessao deslizante.
 */
class CorsConfigTest {

  private CorsConfiguration configuracaoPara(String origemConfigurada, String uri) {
    CorsConfigurationSource source = new CorsConfig(origemConfigurada).corsConfigurationSource();
    return source.getCorsConfiguration(new MockHttpServletRequest("GET", uri));
  }

  @Test
  void deveAplicarAOrigemConfiguradaNasRotasDaApi() {
    CorsConfiguration configuracao =
        configuracaoPara("https://escola.exemplo.com.br", "/api/v1/professor/me");

    assertThat(configuracao).isNotNull();
    assertThat(configuracao.getAllowedOrigins()).containsExactly("https://escola.exemplo.com.br");
    assertThat(configuracao.getAllowedMethods())
        .containsExactly("GET", "POST", "PUT", "DELETE", "OPTIONS");
    assertThat(configuracao.getAllowedHeaders()).containsExactly("Authorization", "Content-Type");
  }

  @Test
  void deveExporOHeaderDoTokenRenovadoParaOFrontendLerASessaoDeslizante() {
    CorsConfiguration configuracao =
        configuracaoPara("http://localhost:8080", "/api/v1/professor/alunos");

    assertThat(configuracao.getExposedHeaders())
        .containsExactly(ProfessorJwtAuthenticationFilter.HEADER_TOKEN_RENOVADO);
  }

  @Test
  void deveAceitarMultiplasOrigensSeparadasPorVirgula() {
    CorsConfiguration configuracao =
        configuracaoPara("http://localhost:8080, https://escola.exemplo.com.br", "/api/v1/alunos");

    assertThat(configuracao.getAllowedOrigins())
        .containsExactly("http://localhost:8080", "https://escola.exemplo.com.br");
  }

  @Test
  void naoDeveAplicarCorsForaDeApiV1() {
    CorsConfiguration configuracao = configuracaoPara("http://localhost:8080", "/internal/v1/algo");

    assertThat(configuracao).isNull();
  }
}
