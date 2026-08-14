/**
 * Visualizador simples de PDF (T092): incorpora o PDF em um iframe e oferece um link direto para
 * abrir/baixar em nova aba. Suficiente para o MVP — sem integracao com PDF.js.
 *
 * As classes sao neutras de proposito: quem estiliza e a area que hospeda o componente (a
 * secretaria em `historico-aluno.css`, o portal no seu proprio CSS), porque cada area define o
 * enquadramento do documento e o componente e usado nas duas.
 */
export interface PdfViewerProps {
  pdfUrl: string;
  titulo?: string;
}

function PdfViewer({ pdfUrl, titulo = 'Visualizar PDF' }: PdfViewerProps): JSX.Element {
  return (
    <div className="visualizador-pdf">
      <a
        className="visualizador-pdf__link"
        href={pdfUrl}
        target="_blank"
        rel="noopener noreferrer"
      >
        {titulo}
      </a>
      <iframe src={pdfUrl} title={titulo} width="100%" height="480" />
    </div>
  );
}

export default PdfViewer;
