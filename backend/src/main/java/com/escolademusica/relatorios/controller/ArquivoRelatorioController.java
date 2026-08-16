package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.service.PdfGeracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serve os PDFs de relatorio gravados no volume de storage ({@code PDF_STORAGE_PATH}).
 *
 * <p>Sem este endpoint o {@code pdfUrl} devolvido pela API nao correspondia a nenhuma rota HTTP: o
 * navegador resolvia o caminho contra a origem do frontend e o fallback SPA do Nginx respondia com
 * o {@code index.html} (tela de login) no lugar do documento.
 *
 * <p>O acesso exige um JWT valido — de administrador (secretaria) ou de professor —, validado pela
 * cadeia de seguranca de {@code /api/v1/arquivos/**} em {@code SecurityConfig}. Como um {@code
 * <iframe>} ou {@code <a href>} nao envia o header {@code Authorization}, o frontend busca o
 * arquivo via {@code fetch} autenticado e o exibe a partir de uma {@code blob:} URL.
 */
@RestController
@RequestMapping(PdfGeracaoService.URL_BASE_PDF)
@Tag(name = "Arquivos", description = "Download dos PDFs de relatorio gerados")
public class ArquivoRelatorioController {

  /**
   * Nomes aceitos, exatamente no formato produzido por {@code PdfGeracaoService}. A validacao e o
   * que impede path traversal: qualquer nome com {@code ../}, barra ou extensao diferente e
   * recusado antes de tocar o filesystem.
   */
  private static final Pattern NOME_ARQUIVO_VALIDO =
      Pattern.compile("relatorio-(aula|semestral)-[0-9a-f-]{36}-v\\d+\\.pdf");

  private final Path storageBasePath;

  public ArquivoRelatorioController(
      @Value("${pdf.storage.path:/app/storage/pdfs}") String storageBasePath) {
    this.storageBasePath = Path.of(storageBasePath);
  }

  @Operation(summary = "Baixa o PDF de um relatorio ja gerado")
  @GetMapping("/{nomeArquivo}")
  public ResponseEntity<byte[]> baixar(@PathVariable String nomeArquivo) throws IOException {
    if (!NOME_ARQUIVO_VALIDO.matcher(nomeArquivo).matches()) {
      return ResponseEntity.notFound().build();
    }

    Path caminhoArquivo = storageBasePath.resolve(nomeArquivo);
    if (!Files.isRegularFile(caminhoArquivo)) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nomeArquivo + "\"")
        .body(Files.readAllBytes(caminhoArquivo));
  }
}
