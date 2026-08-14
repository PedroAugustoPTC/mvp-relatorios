import { useState } from 'react';
import PdfViewer from '../../components/PdfViewer';

/**
 * Conferencia e decisao sobre o PDF gerado (T045), reaproveitado pelos fluxos de relatorio de aula
 * (US1) e semestral (US3).
 *
 * O professor so sai daqui por uma acao explicita — aprovar, pedir alteracao em linguagem natural
 * ou cancelar. A aprovacao envia a `versao` exibida junto: se uma revisao mais nova tiver sido
 * gerada nesse meio tempo, o backend recusa com 409 em vez de aprovar um PDF desatualizado.
 */

export interface VisualizadorPdfRevisaoProps {
  pdfUrl: string | null;
  versao: number;
  ocupado?: boolean;
  onAprovar: (versaoConfirmada: number) => void;
  onRevisar: (instrucao: string) => void;
  onCancelar: () => void;
}

function VisualizadorPdfRevisao({
  pdfUrl,
  versao,
  ocupado = false,
  onAprovar,
  onRevisar,
  onCancelar,
}: VisualizadorPdfRevisaoProps): JSX.Element {
  const [instrucao, setInstrucao] = useState('');

  return (
    <section className="portal-card" style={{ marginTop: 16 }}>
      <h3>Confira o relatório antes de aprovar</h3>
      <p>Versão {versao}</p>

      {pdfUrl ? (
        <PdfViewer pdfUrl={pdfUrl} titulo="Abrir o PDF do relatório" />
      ) : (
        <p role="status" aria-live="polite">
          O PDF ainda está sendo gerado.
        </p>
      )}

      <div style={{ marginTop: 16 }}>
        <label className="portal-rotulo" htmlFor="portal-instrucao-revisao">
          Precisa ajustar algo? Descreva a alteração com suas palavras.
        </label>
        <textarea
          id="portal-instrucao-revisao"
          className="portal-campo"
          rows={3}
          value={instrucao}
          onChange={(evento) => setInstrucao(evento.target.value)}
          placeholder="Ex.: o aluno teve mais facilidade em leitura do que consta nas dificuldades."
          disabled={ocupado}
        />
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 16 }}>
        <button
          type="button"
          className="portal-botao"
          onClick={() => onAprovar(versao)}
          disabled={ocupado || !pdfUrl}
        >
          Aprovar relatório
        </button>
        <button
          type="button"
          className="portal-botao portal-botao--secundario"
          onClick={() => {
            onRevisar(instrucao.trim());
            setInstrucao('');
          }}
          disabled={ocupado || instrucao.trim().length === 0}
        >
          Solicitar alteração
        </button>
        <button
          type="button"
          className="portal-botao portal-botao--secundario"
          onClick={onCancelar}
          disabled={ocupado}
        >
          Cancelar relatório
        </button>
      </div>
    </section>
  );
}

export default VisualizadorPdfRevisao;
