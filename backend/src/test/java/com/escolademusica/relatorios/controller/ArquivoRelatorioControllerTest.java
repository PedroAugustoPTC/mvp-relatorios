package com.escolademusica.relatorios.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Testes de fatia de {@link ArquivoRelatorioController}: o download do PDF a partir do volume de
 * storage e a validacao do nome do arquivo, que e o que impede path traversal.
 */
@WebMvcTest(controllers = ArquivoRelatorioController.class)
@AutoConfigureMockMvc(addFilters = false)
class ArquivoRelatorioControllerTest {

  @TempDir static Path storage;

  @Autowired private MockMvc mockMvc;

  @DynamicPropertySource
  static void configurarStorage(DynamicPropertyRegistry registry) {
    registry.add("pdf.storage.path", () -> storage.toString());
  }

  @Test
  void devolveOsBytesDoPdfGravadoNoStorage() throws Exception {
    String nomeArquivo = "relatorio-aula-%s-v1.pdf".formatted(UUID.randomUUID());
    Files.write(storage.resolve(nomeArquivo), "%PDF-1.4 conteudo".getBytes());

    mockMvc
        .perform(get("/api/v1/arquivos/relatorios/{nomeArquivo}", nomeArquivo))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(
            header()
                .string("Content-Disposition", "inline; filename=\"%s\"".formatted(nomeArquivo)))
        .andExpect(content().bytes("%PDF-1.4 conteudo".getBytes()));
  }

  @Test
  void devolve404QuandoOArquivoNaoExisteNoStorage() throws Exception {
    String nomeArquivo = "relatorio-semestral-%s-v2.pdf".formatted(UUID.randomUUID());

    mockMvc
        .perform(get("/api/v1/arquivos/relatorios/{nomeArquivo}", nomeArquivo))
        .andExpect(status().isNotFound());
  }

  @Test
  void recusaNomeDeArquivoForaDoPadraoSemTocarNoFilesystem() throws Exception {
    Files.write(storage.resolve("segredo.pdf"), "nao deveria vazar".getBytes());

    mockMvc
        .perform(get("/api/v1/arquivos/relatorios/{nomeArquivo}", "segredo.pdf"))
        .andExpect(status().isNotFound());
  }
}
