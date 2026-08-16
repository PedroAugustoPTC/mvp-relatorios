package com.escolademusica.relatorios.service;

import com.escolademusica.relatorios.dto.RelatorioAulaPdfConteudoDto;
import com.escolademusica.relatorios.dto.RelatorioSemestralPdfConteudoDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Servico base de geracao de PDF (research.md secao 4): persiste o PDF de um relatorio (de aula ou
 * semestral) no volume de storage configurado e retorna o {@code pdfUrl} (caminho relativo) a ser
 * gravado no registro correspondente.
 *
 * <p>A renderizacao real dos bytes do PDF usa OpenPDF ({@code com.github.librepdf:openpdf}, ja
 * declarado em {@code backend/pom.xml}), tanto para o relatorio de aula (T046) quanto para o
 * relatorio semestral (T078).
 */
@Service
public class PdfGeracaoService {

  /**
   * Prefixo da URL HTTP sob a qual os PDFs gravados no storage sao servidos (ver {@code
   * ArquivoRelatorioController}). O {@code pdfUrl} persistido no relatorio precisa ser uma URL que
   * o navegador consiga buscar — nao o caminho de filesystem do container, que resolveria contra a
   * origem do frontend e cairia no fallback SPA do Nginx (devolvendo o index.html no lugar do PDF).
   */
  public static final String URL_BASE_PDF = "/api/v1/arquivos/relatorios";

  private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final Path storageBasePath;
  private final String nomeEscola;

  public PdfGeracaoService(
      @Value("${pdf.storage.path:/app/storage/pdfs}") String storageBasePath,
      @Value("${escola.nome:Escola de Música}") String nomeEscola) {
    this.storageBasePath = Path.of(storageBasePath);
    this.nomeEscola = nomeEscola;
  }

  /**
   * Gera (ou tenta gerar) o PDF de um relatorio de aula e o grava no storage, retornando o pdfUrl
   * relativo para persistir em {@code RelatorioAula.pdfUrl}.
   *
   * @param relatorioAulaId id do relatorio de aula (usado para nomear o arquivo)
   * @param versao versao do relatorio (FR-009); cada nova versao gera um novo arquivo
   * @param conteudo dados ja estruturados do relatorio, no formato esperado por {@link
   *     #renderPdfBytes} (implementacao futura)
   * @return caminho relativo do PDF gerado dentro do storage configurado
   */
  public String gerarPdfRelatorioAula(UUID relatorioAulaId, int versao, Object conteudo) {
    String nomeArquivo = "relatorio-aula-%s-v%d.pdf".formatted(relatorioAulaId, versao);
    return gerarEArmazenar(nomeArquivo, conteudo);
  }

  /**
   * Gera (ou tenta gerar) o PDF de um relatorio semestral e o grava no storage, retornando o pdfUrl
   * relativo para persistir em {@code RelatorioSemestral.pdfUrl}.
   *
   * @param relatorioSemestralId id do relatorio semestral (usado para nomear o arquivo)
   * @param versao versao do relatorio (FR-015); cada nova versao gera um novo arquivo
   * @param conteudo dados ja estruturados do relatorio, no formato esperado por {@link
   *     #renderPdfBytes} (implementacao futura)
   * @return caminho relativo do PDF gerado dentro do storage configurado
   */
  public String gerarPdfRelatorioSemestral(UUID relatorioSemestralId, int versao, Object conteudo) {
    String nomeArquivo = "relatorio-semestral-%s-v%d.pdf".formatted(relatorioSemestralId, versao);
    return gerarEArmazenar(nomeArquivo, conteudo);
  }

  /** Plumbing comum: renderiza os bytes, garante o diretorio, grava o arquivo e retorna a URL. */
  private String gerarEArmazenar(String nomeArquivo, Object conteudo) {
    byte[] bytesPdf = renderPdfBytes(conteudo);

    try {
      Files.createDirectories(storageBasePath);
      Path caminhoArquivo = storageBasePath.resolve(nomeArquivo);
      Files.write(caminhoArquivo, bytesPdf);
      return URL_BASE_PDF + "/" + nomeArquivo;
    } catch (IOException e) {
      throw new UncheckedIOException("Falha ao gravar PDF no storage: " + nomeArquivo, e);
    }
  }

  /**
   * Renderiza o conteudo estruturado do relatorio para bytes de PDF.
   *
   * <p>Suporta o relatorio de aula ({@link RelatorioAulaPdfConteudoDto}, T046) e o relatorio
   * semestral ({@link RelatorioSemestralPdfConteudoDto}, T078).
   */
  byte[] renderPdfBytes(Object conteudo) {
    if (conteudo instanceof RelatorioAulaPdfConteudoDto relatorioAula) {
      return renderRelatorioAula(relatorioAula);
    }
    if (conteudo instanceof RelatorioSemestralPdfConteudoDto relatorioSemestral) {
      return renderRelatorioSemestral(relatorioSemestral);
    }
    throw new UnsupportedOperationException(
        "Renderizacao de PDF para este tipo de conteudo ainda nao implementada: "
            + (conteudo == null ? "null" : conteudo.getClass()));
  }

  /** Layout simples do relatorio de aula (FR-006, FR-008): cabecalho + secoes do conteudo. */
  private byte[] renderRelatorioAula(RelatorioAulaPdfConteudoDto conteudo) {
    Document documento = new Document();
    ByteArrayOutputStream saida = new ByteArrayOutputStream();
    try {
      PdfWriter.getInstance(documento, saida);
      documento.open();

      Font fonteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
      Font fonteSecao = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
      Font fonteNormal = FontFactory.getFont(FontFactory.HELVETICA, 11);

      Paragraph titulo = new Paragraph("Relatorio de Aula", fonteTitulo);
      titulo.setSpacingAfter(10f);
      documento.add(titulo);

      Paragraph cabecalho =
          new Paragraph(
              "Aluno: %s\nProfessor: %s\nData da aula: %s"
                  .formatted(
                      valorOuTraco(conteudo.nomeAluno()),
                      valorOuTraco(conteudo.nomeProfessor()),
                      conteudo.dataAula() == null ? "-" : conteudo.dataAula().format(FORMATO_DATA)),
              fonteNormal);
      cabecalho.setSpacingAfter(14f);
      documento.add(cabecalho);

      adicionarSecaoLista(
          documento,
          "Conteudos Trabalhados",
          conteudo.conteudosTrabalhados(),
          fonteSecao,
          fonteNormal);
      adicionarSecaoTexto(documento, "Evolucao", conteudo.evolucao(), fonteSecao, fonteNormal);
      adicionarSecaoLista(
          documento, "Dificuldades", conteudo.dificuldades(), fonteSecao, fonteNormal);
      adicionarSecaoLista(
          documento,
          "Atividades Propostas",
          conteudo.atividadesPropostas(),
          fonteSecao,
          fonteNormal);
      adicionarSecaoTexto(
          documento, "Observacoes", conteudo.observacoes(), fonteSecao, fonteNormal);

      documento.close();
      return saida.toByteArray();
    } catch (DocumentException e) {
      throw new IllegalStateException("Falha ao gerar PDF do relatorio de aula", e);
    }
  }

  private void adicionarSecaoTexto(
      Document documento, String titulo, String texto, Font fonteSecao, Font fonteNormal)
      throws DocumentException {
    Paragraph tituloSecao = new Paragraph(titulo, fonteSecao);
    tituloSecao.setSpacingBefore(8f);
    documento.add(tituloSecao);
    documento.add(new Paragraph(valorOuTraco(texto), fonteNormal));
  }

  private void adicionarSecaoLista(
      Document documento, String titulo, List<String> itens, Font fonteSecao, Font fonteNormal)
      throws DocumentException {
    Paragraph tituloSecao = new Paragraph(titulo, fonteSecao);
    tituloSecao.setSpacingBefore(8f);
    documento.add(tituloSecao);
    if (itens == null || itens.isEmpty()) {
      documento.add(new Paragraph("-", fonteNormal));
      return;
    }
    com.lowagie.text.List lista = new com.lowagie.text.List(false, 10);
    lista.setListSymbol("- ");
    for (String item : itens) {
      lista.add(new com.lowagie.text.ListItem(item, fonteNormal));
    }
    documento.add((Element) lista);
  }

  private String valorOuTraco(String valor) {
    return (valor == null || valor.isBlank()) ? "-" : valor;
  }

  /**
   * Layout completo do relatorio semestral (FR-013): cabecalho/rodape com o nome da escola
   * (configuravel via {@code escola.nome}) e uma secao por bloco do modelo de referencia, incluindo
   * o parecer final do professor.
   */
  private byte[] renderRelatorioSemestral(RelatorioSemestralPdfConteudoDto conteudo) {
    Document documento = new Document();
    ByteArrayOutputStream saida = new ByteArrayOutputStream();
    try {
      PdfWriter.getInstance(documento, saida);
      documento.open();

      Font fonteEscola = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
      Font fonteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
      Font fonteSecao = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
      Font fonteNormal = FontFactory.getFont(FontFactory.HELVETICA, 11);
      Font fonteRodape = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);

      Paragraph cabecalhoEscola = new Paragraph(nomeEscola, fonteEscola);
      cabecalhoEscola.setSpacingAfter(6f);
      documento.add(cabecalhoEscola);

      Paragraph titulo = new Paragraph("Relatorio Semestral", fonteTitulo);
      titulo.setSpacingAfter(10f);
      documento.add(titulo);

      Paragraph resumo =
          new Paragraph(
              "Aluno: %s\nProfessor: %s\nPeriodo: %s (%s a %s)\nRelatorios de aula considerados: %d"
                  .formatted(
                      valorOuTraco(conteudo.nomeAluno()),
                      valorOuTraco(conteudo.nomeProfessor()),
                      valorOuTraco(conteudo.rotuloPeriodo()),
                      conteudo.periodoInicio() == null
                          ? "-"
                          : conteudo.periodoInicio().format(FORMATO_DATA),
                      conteudo.periodoFim() == null
                          ? "-"
                          : conteudo.periodoFim().format(FORMATO_DATA),
                      conteudo.quantidadeRelatoriosAulaConsiderados()),
              fonteNormal);
      resumo.setSpacingAfter(14f);
      documento.add(resumo);

      adicionarSecaoJson(
          documento, "Informacoes Gerais", conteudo.informacoesGerais(), fonteSecao, fonteNormal);
      adicionarSecaoJson(
          documento, "Frequencia e Estudo", conteudo.frequenciaEEstudo(), fonteSecao, fonteNormal);
      adicionarSecaoJson(documento, "Tecnica", conteudo.tecnica(), fonteSecao, fonteNormal);
      adicionarSecaoJson(
          documento, "Musicalidade", conteudo.musicalidade(), fonteSecao, fonteNormal);
      adicionarSecaoJson(
          documento,
          "Leitura e Memorizacao",
          conteudo.leituraEMemorizacao(),
          fonteSecao,
          fonteNormal);
      adicionarSecaoJson(
          documento, "Pontos de Atencao", conteudo.pontosDeAtencao(), fonteSecao, fonteNormal);
      adicionarSecaoJson(
          documento,
          "Estrategias Pedagogicas",
          conteudo.estrategiasPedagogicas(),
          fonteSecao,
          fonteNormal);
      adicionarSecaoJson(
          documento,
          "Acompanhamento Familiar",
          conteudo.acompanhamentoFamiliar(),
          fonteSecao,
          fonteNormal);
      adicionarSecaoJson(
          documento,
          "Planejamento do Proximo Semestre",
          conteudo.planejamentoProximoSemestre(),
          fonteSecao,
          fonteNormal);

      Paragraph tituloParecer = new Paragraph("Parecer Final", fonteSecao);
      tituloParecer.setSpacingBefore(8f);
      documento.add(tituloParecer);
      documento.add(new Paragraph(valorOuTraco(conteudo.parecerFinal()), fonteNormal));

      Paragraph rodape =
          new Paragraph(
              "%s - documento gerado automaticamente a partir dos relatorios de aula do periodo."
                  .formatted(nomeEscola),
              fonteRodape);
      rodape.setSpacingBefore(16f);
      documento.add(rodape);

      documento.close();
      return saida.toByteArray();
    } catch (DocumentException e) {
      throw new IllegalStateException("Falha ao gerar PDF do relatorio semestral", e);
    }
  }

  /**
   * Renderiza uma secao cujo conteudo e um {@link JsonNode} generico (objeto com sub-campos, array,
   * ou texto simples), convertendo cada sub-campo em uma linha "rotulo: valor".
   */
  private void adicionarSecaoJson(
      Document documento, String titulo, JsonNode no, Font fonteSecao, Font fonteNormal)
      throws DocumentException {
    Paragraph tituloSecao = new Paragraph(titulo, fonteSecao);
    tituloSecao.setSpacingBefore(8f);
    documento.add(tituloSecao);

    if (no == null || no.isNull() || no.isMissingNode()) {
      documento.add(new Paragraph("-", fonteNormal));
      return;
    }
    if (no.isObject()) {
      com.lowagie.text.List lista = new com.lowagie.text.List(false, 10);
      lista.setListSymbol("- ");
      Iterator<Map.Entry<String, JsonNode>> campos = no.fields();
      boolean algumCampo = false;
      while (campos.hasNext()) {
        Map.Entry<String, JsonNode> campo = campos.next();
        algumCampo = true;
        lista.add(
            new com.lowagie.text.ListItem(
                "%s: %s".formatted(campo.getKey(), valorDeJson(campo.getValue())), fonteNormal));
      }
      if (!algumCampo) {
        documento.add(new Paragraph("-", fonteNormal));
      } else {
        documento.add((Element) lista);
      }
      return;
    }
    if (no.isArray()) {
      if (no.isEmpty()) {
        documento.add(new Paragraph("-", fonteNormal));
        return;
      }
      com.lowagie.text.List lista = new com.lowagie.text.List(false, 10);
      lista.setListSymbol("- ");
      for (JsonNode item : no) {
        lista.add(new com.lowagie.text.ListItem(valorDeJson(item), fonteNormal));
      }
      documento.add((Element) lista);
      return;
    }
    documento.add(new Paragraph(valorOuTraco(no.asText()), fonteNormal));
  }

  /** Converte um JsonNode filho (texto, array ou objeto aninhado) em uma representacao textual. */
  private String valorDeJson(JsonNode no) {
    if (no == null || no.isNull() || no.isMissingNode()) {
      return "-";
    }
    if (no.isTextual()) {
      return valorOuTraco(no.asText());
    }
    if (no.isArray()) {
      StringBuilder sb = new StringBuilder();
      for (JsonNode item : no) {
        if (sb.length() > 0) {
          sb.append("; ");
        }
        sb.append(valorDeJson(item));
      }
      return sb.length() == 0 ? "-" : sb.toString();
    }
    return no.toString();
  }
}
