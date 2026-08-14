/**
 * Visualizador simples de PDF (T092): incorpora o PDF em um iframe e oferece um link direto para
 * abrir/baixar em nova aba. Suficiente para o MVP — sem integracao com PDF.js.
 */
export interface PdfViewerProps {
  pdfUrl: string;
  titulo?: string;
}

function PdfViewer({ pdfUrl, titulo = 'Visualizar PDF' }: PdfViewerProps): JSX.Element {
  return (
    <div>
      <p>
        <a href={pdfUrl} target="_blank" rel="noopener noreferrer">
          {titulo}
        </a>
      </p>
      <iframe src={pdfUrl} title={titulo} width="100%" height="480" />
    </div>
  );
}

export default PdfViewer;
