import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import VisualizadorPdfRevisao from '../components/VisualizadorPdfRevisao';
import {
  consultarRascunhoPendente,
  RascunhoPendente,
} from '../services/professorPortalService';
import {
  aprovarRelatorioAula,
  cancelarRelatorioAula,
  consultarRelatorioAula,
  RelatorioAulaPortal,
  responderPergunta,
  revisarRelatorioAula,
} from '../services/relatorioPortalService';

/**
 * Retomada de um relatorio deixado em aberto (T046, FR-022a).
 *
 * O rascunho pode ter sido iniciado pelo bot do Telegram e continuado aqui — por isso a tela diz
 * explicitamente de qual canal ele veio. O professor decide: continuar de onde parou ou descartar e
 * comecar de novo. Nada e decidido automaticamente por ele.
 */
function RetomarRascunho(): JSX.Element {
  const { alunoId = '' } = useParams();
  const navigate = useNavigate();

  const [pendente, setPendente] = useState<RascunhoPendente | null>(null);
  const [relatorio, setRelatorio] = useState<RelatorioAulaPortal | null>(null);
  const [continuando, setContinuando] = useState(false);
  const [resposta, setResposta] = useState('');
  const [ocupado, setOcupado] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    consultarRascunhoPendente(alunoId)
      .then((resultado) => {
        if (!ativo) {
          return;
        }
        setPendente(resultado);
        if (!resultado.existeRascunho) {
          navigate(`/portal/alunos/${alunoId}/novo-relatorio`, { replace: true });
        }
      })
      .catch((e: unknown) =>
        setErro(e instanceof Error ? e.message : 'Não foi possível verificar rascunhos pendentes.'),
      );
    return () => {
      ativo = false;
    };
  }, [alunoId, navigate]);

  async function executar<T>(acao: () => Promise<T>): Promise<T | null> {
    setOcupado(true);
    setErro(null);
    try {
      return await acao();
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Não foi possível concluir a ação.');
      return null;
    } finally {
      setOcupado(false);
    }
  }

  async function continuar(): Promise<void> {
    if (!pendente?.relatorioId) {
      return;
    }
    const atual = await executar(() => consultarRelatorioAula(pendente.relatorioId as string));
    if (atual) {
      setRelatorio(atual);
      setContinuando(true);
    }
  }

  async function descartar(): Promise<void> {
    if (!pendente?.relatorioId) {
      return;
    }
    const resultado = await executar(() =>
      cancelarRelatorioAula(pendente.relatorioId as string),
    );
    if (resultado !== null) {
      navigate(`/portal/alunos/${alunoId}/novo-relatorio`, { replace: true });
    }
  }

  if (erro && !pendente) {
    return (
      <p className="portal-erro" role="alert">
        {erro}
      </p>
    );
  }

  if (!pendente) {
    return (
      <p role="status" aria-live="polite">
        Verificando relatórios em aberto...
      </p>
    );
  }

  const origem = pendente.canalOrigem === 'TELEGRAM' ? 'pelo Telegram' : 'pelo portal web';

  if (!continuando) {
    return (
      <section>
        <h1>Você tem um relatório em aberto</h1>
        <p>
          Este aluno já tem um relatório de aula iniciado {origem} e ainda não concluído
          {pendente.atualizadoEm
            ? ` (última alteração em ${new Date(pendente.atualizadoEm).toLocaleString('pt-BR')})`
            : ''}
          .
        </p>

        {erro && (
          <p className="portal-erro" role="alert">
            {erro}
          </p>
        )}

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 16 }}>
          <button
            type="button"
            className="portal-botao"
            onClick={() => void continuar()}
            disabled={ocupado}
          >
            Continuar de onde parei
          </button>
          <button
            type="button"
            className="portal-botao portal-botao--secundario"
            onClick={() => void descartar()}
            disabled={ocupado}
          >
            Descartar e começar um novo
          </button>
        </div>
      </section>
    );
  }

  if (!relatorio) {
    return (
      <p role="status" aria-live="polite">
        Carregando o relatório...
      </p>
    );
  }

  return (
    <section>
      <h1>Continuando o relatório</h1>

      {erro && (
        <p className="portal-erro" role="alert">
          {erro}
        </p>
      )}

      {relatorio.perguntasPendentes.length > 0 ? (
        <section className="portal-card" style={{ marginTop: 16 }}>
          <h3>Faltam algumas informações</h3>
          <ul>
            {relatorio.perguntasPendentes.map((pergunta) => (
              <li key={pergunta}>{pergunta}</li>
            ))}
          </ul>
          <label className="portal-rotulo" htmlFor="portal-retomar-resposta">
            Sua resposta
          </label>
          <textarea
            id="portal-retomar-resposta"
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
            disabled={ocupado || resposta.trim().length === 0}
            onClick={() => {
              void executar(() =>
                responderPergunta(relatorio.relatorioId, resposta.trim()),
              ).then((atualizado) => {
                if (atualizado) {
                  setResposta('');
                  setRelatorio(atualizado);
                }
              });
            }}
          >
            Enviar resposta
          </button>
        </section>
      ) : (
        <VisualizadorPdfRevisao
          pdfUrl={relatorio.pdfUrl}
          versao={relatorio.versao}
          ocupado={ocupado}
          onAprovar={(versaoConfirmada) => {
            void executar(() =>
              aprovarRelatorioAula(relatorio.relatorioId, versaoConfirmada),
            ).then((resultado) => {
              if (resultado) {
                navigate(`/portal/alunos/${alunoId}/historico`);
              }
            });
          }}
          onRevisar={(instrucao) => {
            void executar(() => revisarRelatorioAula(relatorio.relatorioId, instrucao)).then(
              (atualizado) => {
                if (atualizado) {
                  setRelatorio(atualizado);
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

export default RetomarRascunho;
