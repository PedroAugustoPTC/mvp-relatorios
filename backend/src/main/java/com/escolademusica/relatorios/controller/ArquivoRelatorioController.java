package com.escolademusica.relatorios.controller;

import com.escolademusica.relatorios.service.PdfGeracaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

  private final PdfGeracaoService pdfGeracaoService;

  public ArquivoRelatorioController(PdfGeracaoService pdfGeracaoService) {
    this.pdfGeracaoService = pdfGeracaoService;
  }

  @Operation(summary = "Baixa o PDF de um relatorio ja gerado")
  @GetMapping("/{nomeArquivo}")
  public ResponseEntity<byte[]> baixar(@PathVariable String nomeArquivo) {
    return pdfGeracaoService
        .lerPdf(nomeArquivo)
        .map(
            bytes ->
                ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nomeArquivo + "\"")
                    .body(bytes))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
