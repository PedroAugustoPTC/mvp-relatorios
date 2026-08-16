import { useEffect, useState } from 'react';

/**
 * Visualizador simples de PDF (T092): incorpora o PDF em um iframe e oferece um link direto para
 * abrir/baixar em nova aba. Suficiente para o MVP — sem integracao com PDF.js.
 *
 * O PDF NAO pode ser apontado diretamente pelo `src` do iframe: a rota que o serve
 * (`/api/v1/arquivos/relatorios/...`) exige `Authorization`, header que iframes e links nao enviam.
 * Por isso o componente busca o arquivo via `baixarPdf` (fetch autenticado, injetado pela area que
 * hospeda o componente porque secretaria e portal usam tokens diferentes) e exibe o resultado a
 * partir de uma `blob:` URL, revogada quando o componente sai de cena ou o PDF muda de versao.
 *
 * As classes sao neutras de proposito: quem estiliza e a area que hospeda o componente (a
 * secretaria em `historico-aluno.css`, o portal no seu proprio CSS), porque cada area define o
 * enquadramento do documento e o componente e usado nas duas.
 */
export interface PdfViewerProps {
  pdfUrl: string;
  titulo?: string;
  /** Busca o PDF com o token da sessao vigente (apiClient para a secretaria, professorApiClient
   * para o portal). */
  baixarPdf: (url: string) => Promise<Blob>;
}

function PdfViewer({ pdfUrl, titulo = 'Visualizar PDF', baixarPdf }: PdfViewerProps): JSX.Element {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let cancelado = false;
    let urlCriada: string | null = null;

    setBlobUrl(null);
    setErro(null);

    baixarPdf(pdfUrl)
      .then((blob) => {
        if (cancelado) {
          return;
        }
        urlCriada = URL.createObjectURL(blob);
        setBlobUrl(urlCriada);
      })
      .catch(() => {
        if (!cancelado) {
          setErro('Nao foi possivel carregar o PDF. Tente novamente.');
        }
      });

    return () => {
      cancelado = true;
      if (urlCriada) {
        URL.revokeObjectURL(urlCriada);
      }
    };
  }, [pdfUrl, baixarPdf]);

  return (
    <div className="visualizador-pdf">
      {blobUrl ? (
        <>
          <a
            className="visualizador-pdf__link"
            href={blobUrl}
            target="_blank"
            rel="noopener noreferrer"
          >
            {titulo}
          </a>
          <iframe src={blobUrl} title={titulo} width="100%" height="480" />
        </>
      ) : (
        <p className="visualizador-pdf__estado" role="status" aria-live="polite">
          {erro ?? `${titulo} — carregando…`}
        </p>
      )}
    </div>
  );
}

export default PdfViewer;
