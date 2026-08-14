import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import GravadorAudio from '../components/GravadorAudio';
import VisualizadorPdfRevisao from '../components/VisualizadorPdfRevisao';
import { usePollingProcessamento } from '../hooks/usePollingProcessamento';
import { consultarRascunhoPendente } from '../services/professorPortalService';
import {
  aprovarRelatorioAula,
  cancelarRelatorioAula,
  enviarAudio,
  responderPergunta,
  revisarRelatorioAula,
} from '../services/relatorioPortalService';

/**
 * Registro de uma aula pelo portal (T044, US1): enviar/gravar o audio, acompanhar o processamento,
 * responder o que faltou e conferir o PDF antes de aprovar.
 *
 * Antes de comecar, verifica se ja existe um relatorio em aberto para este aluno em qualquer canal
 * (FR-022a) e leva o professor para a tela de retomada — evitando dois relatorios paralelos para a
 * mesma aula.
 */
function NovoRelatorioAula(): JSX.Element {
  const { alunoId = '' } = useParams();
  const navigate = useNavigate();

  const { passo, rotuloPasso, relatorio, erro, iniciar, definirRelatorio, definirPasso, definirErro } =
    usePollingProcessamento();

  const [audio, setAudio] = useState<{ blob: Blob; nome: string } | null>(null);
  const [resposta, setResposta] = useState('');
  const [ocupado, setOcupado] = useState(false);
  const [aprovado, setAprovado] = useState(false);

  // FR-022a: rascunho pendente em qualquer canal tem precedencia sobre iniciar um novo relatorio.
  useEffect(() => {
    let ativo = true;
    consultarRascunhoPendente(alunoId)
      .then((pendente) => {
        if (ativo && pendente.existeRascunho && pendente.tipo === 'AULA') {
          navigate(`/portal/alunos/${alunoId}/retomar-rascunho`, { replace: true });
        }
      })
      .catch(() => {
        // A verificacao e um facilitador, nao um bloqueio: se falhar, o professor segue o fluxo
        // normal e o backend continua sendo a fonte de verdade.
      });
    return () => {
      ativo = false;
    };
  }, [alunoId, navigate]);

  async function executar<T>(acao: () => Promise<T>): Promise<T | null> {
    setOcupado(true);
    definirErro(null);
    try {
      return await acao();
    } catch (e) {
      definirErro(e instanceof Error ? e.message : 'Não foi possível concluir a ação.');
      return null;
    } finally {
      setOcupado(false);
    }
  }

  async function enviar(): Promise<void> {
    if (!audio) {
      return;
    }
    definirPasso('enviando');
    const resultado = await executar(() => enviarAudio(alunoId, audio.blob, audio.nome));
    if (!resultado) {
      definirPasso('erro');
      return;
    }
    // O backend ja devolve o estado real; o polling cobre o caso de ele ainda estar em andamento.
    iniciar(resultado.relatorioId);
  }

  async function enviarResposta(): Promise<void> {
    if (!relatorio || resposta.trim().length === 0) {
      return;
    }
    const atualizado = await executar(() =>
      responderPergunta(relatorio.relatorioId, resposta.trim()),
    );
    if (atualizado) {
      setResposta('');
      definirRelatorio(atualizado);
    }
  }

  if (aprovado) {
    return (
      <section>
        <h1>Relatório aprovado</h1>
        <p role="status" aria-live="polite">
          O relatório desta aula foi aprovado e já consta no histórico do aluno.
        </p>
        <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
          <button
            type="button"
            className="portal-botao"
            onClick={() => navigate(`/portal/alunos/${alunoId}/historico`)}
          >
            Ver histórico do aluno
          </button>
          <button
            type="button"
            className="portal-botao portal-botao--secundario"
            onClick={() => navigate('/portal/alunos')}
          >
            Voltar aos alunos
          </button>
        </div>
      </section>
    );
  }

  const emProcessamento = passo === 'enviando' || passo === 'processando';

  return (
    <section>
      <h1>Novo relatório de aula</h1>

      {!relatorio && (
        <>
          <GravadorAudio
            desabilitado={ocupado || emProcessamento}
            onAudioPronto={(blob, nome) => setAudio({ blob, nome })}
          />
          <button
            type="button"
            className="portal-botao"
            style={{ marginTop: 16 }}
            onClick={() => void enviar()}
            disabled={!audio || ocupado || emProcessamento}
          >
            Enviar áudio e gerar relatório
          </button>
        </>
      )}

      {/* Feedback progressivo: a tela nunca fica parada sem explicar o que esta acontecendo. */}
      {rotuloPasso && (
        <p role="status" aria-live="polite" style={{ marginTop: 16 }}>
          {rotuloPasso}
        </p>
      )}

      {erro && (
        <p className="portal-erro" role="alert">
          {erro}
        </p>
      )}

      {relatorio && relatorio.perguntasPendentes.length > 0 && (
        <section className="portal-card" style={{ marginTop: 16 }}>
          <h3>Faltam algumas informações</h3>
          <ul>
            {relatorio.perguntasPendentes.map((pergunta) => (
              <li key={pergunta}>{pergunta}</li>
            ))}
          </ul>
          <label className="portal-rotulo" htmlFor="portal-resposta-pergunta">
            Sua resposta
          </label>
          <textarea
            id="portal-resposta-pergunta"
            className="portal-campo"
            rows={3}
            value={resposta}
            onChange={(evento) => setResposta(evento.target.value)}
            disabled={ocupado}
          />
          <button
            type="button"
            className="portal-botao"
            style={{ marginTop: 12 }}
            onClick={() => void enviarResposta()}
            disabled={ocupado || resposta.trim().length === 0}
          >
            Enviar resposta
          </button>
        </section>
      )}

      {relatorio && relatorio.perguntasPendentes.length === 0 && (
        <VisualizadorPdfRevisao
          pdfUrl={relatorio.pdfUrl}
          versao={relatorio.versao}
          ocupado={ocupado}
          onAprovar={(versaoConfirmada) => {
            void executar(() => aprovarRelatorioAula(relatorio.relatorioId, versaoConfirmada)).then(
              (resultado) => {
                if (resultado) {
                  setAprovado(true);
                }
              },
            );
          }}
          onRevisar={(instrucao) => {
            void executar(() => revisarRelatorioAula(relatorio.relatorioId, instrucao)).then(
              (atualizado) => {
                if (atualizado) {
                  definirRelatorio(atualizado);
                }
              },
            );
          }}
          onCancelar={() => {
            void executar(() => cancelarRelatorioAula(relatorio.relatorioId)).then(() =>
              navigate('/portal/alunos'),
            );
          }}
        />
      )}
    </section>
  );
}

export default NovoRelatorioAula;
