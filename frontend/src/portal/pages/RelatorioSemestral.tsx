import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import VisualizadorPdfRevisao from '../components/VisualizadorPdfRevisao';
import { ApiError } from '../services/professorApiClient';
import {
  aprovarRelatorioSemestral,
  cancelarRelatorioSemestral,
  gerarRelatorioSemestral,
  listarSemestresDisponiveis,
  revisarRelatorioSemestral,
  SemestreDisponivel,
} from '../services/relatorioPortalService';
import './relatorio-semestral.css';

/**
 * Relatorio semestral consolidado pelo portal (T071/T072, US3).
 *
 * O seletor de semestre reproduz as pills numeradas da secao "Statistics" da imagem de referencia
 * (research.md secao 7), adaptadas aos semestres do aluno. Apos consolidar, a tela informa quantos
 * relatorios de aula alimentaram o resultado (FR-016) e reaproveita `VisualizadorPdfRevisao` para a
 * conferencia e aprovacao.
 */

interface RelatorioGerado {
  relatorioId: string;
  quantidade: number;
  pdfUrl: string | null;
  versao: number;
}

function RelatorioSemestral(): JSX.Element {
  const { alunoId = '' } = useParams();
  const navigate = useNavigate();

  const [semestres, setSemestres] = useState<SemestreDisponivel[]>([]);
  const [selecionado, setSelecionado] = useState<string | null>(null);
  const [gerado, setGerado] = useState<RelatorioGerado | null>(null);
  const [aprovado, setAprovado] = useState(false);
  const [ocupado, setOcupado] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    let ativo = true;
    listarSemestresDisponiveis(alunoId)
      .then((lista) => {
        if (ativo) {
          setSemestres(lista);
          setSelecionado(lista.length > 0 ? lista[lista.length - 1].chave : null);
        }
      })
      .catch((e: unknown) =>
        setErro(e instanceof Error ? e.message : 'Não foi possível carregar os semestres.'),
      );
    return () => {
      ativo = false;
    };
  }, [alunoId]);

  async function executar<T>(acao: () => Promise<T>): Promise<T | null> {
    setOcupado(true);
    setErro(null);
    try {
      return await acao();
    } catch (e) {
      if (e instanceof ApiError && e.codigo === 'SEM_DADOS_NO_PERIODO') {
        setErro(
          'Nenhum relatório de aula aprovado neste período. Registre as aulas do semestre antes de gerar o relatório.',
        );
      } else {
        setErro(e instanceof Error ? e.message : 'Não foi possível concluir a ação.');
      }
      return null;
    } finally {
      setOcupado(false);
    }
  }

  async function gerar(): Promise<void> {
    if (!selecionado) {
      return;
    }
    const resultado = await executar(() => gerarRelatorioSemestral(alunoId, selecionado));
    if (resultado) {
      setGerado({
        relatorioId: resultado.relatorioSemestralId,
        quantidade: resultado.quantidadeRelatoriosEncontrados,
        pdfUrl: resultado.pdfUrl,
        versao: resultado.versao,
      });
    }
  }

  if (aprovado) {
    return (
      <section>
        <h1>Relatório semestral aprovado</h1>
        <p role="status" aria-live="polite">
          O relatório semestral foi aprovado e já consta no histórico do aluno.
        </p>
        <button
          type="button"
          className="portal-botao"
          style={{ marginTop: 16 }}
          onClick={() => navigate(`/portal/alunos/${alunoId}/historico`)}
        >
          Ver histórico do aluno
        </button>
      </section>
    );
  }

  return (
    <section>
      <h1>Relatório semestral</h1>

      {!gerado && (
        <>
          <p>Escolha o semestre que deseja consolidar.</p>

          {semestres.length === 0 ? (
            <p style={{ marginTop: 16 }}>Nenhum semestre disponível para este aluno ainda.</p>
          ) : (
            <div className="semestre-pills" role="group" aria-label="Semestres disponíveis">
              {semestres.map((semestre) => (
                <button
                  key={semestre.chave}
                  type="button"
                  className="portal-pill"
                  aria-pressed={selecionado === semestre.chave}
                  onClick={() => setSelecionado(semestre.chave)}
                  disabled={ocupado}
                >
                  {semestre.rotulo}
                </button>
              ))}
            </div>
          )}

          <button
            type="button"
            className="portal-botao"
            style={{ marginTop: 20 }}
            onClick={() => void gerar()}
            disabled={ocupado || !selecionado}
          >
            {ocupado ? 'Consolidando o semestre...' : 'Gerar relatório semestral'}
          </button>
        </>
      )}

      {ocupado && (
        <p role="status" aria-live="polite" style={{ marginTop: 16 }}>
          Consolidando os relatórios de aula do período...
        </p>
      )}

      {erro && (
        <p className="portal-erro" role="alert" style={{ marginTop: 16 }}>
          {erro}
        </p>
      )}

      {gerado && (
        <>
          <p style={{ marginTop: 16 }}>
            {gerado.quantidade === 1
              ? '1 relatório de aula foi considerado nesta consolidação.'
              : `${gerado.quantidade} relatórios de aula foram considerados nesta consolidação.`}
          </p>

          <VisualizadorPdfRevisao
            pdfUrl={gerado.pdfUrl}
            versao={gerado.versao}
            ocupado={ocupado}
            onAprovar={(versaoConfirmada) => {
              void executar(() =>
                aprovarRelatorioSemestral(gerado.relatorioId, versaoConfirmada),
              ).then((resultado) => {
                if (resultado) {
                  setAprovado(true);
                }
              });
            }}
            onRevisar={(instrucao) => {
              void executar(() => revisarRelatorioSemestral(gerado.relatorioId, instrucao)).then(
                (atualizado) => {
                  if (atualizado) {
                    setGerado({
                      relatorioId: atualizado.relatorioId,
                      quantidade: atualizado.quantidadeRelatoriosAulaConsiderados,
                      pdfUrl: atualizado.pdfUrl,
                      versao: atualizado.versao,
                    });
                  }
                },
              );
            }}
            onCancelar={() => {
              void executar(() => cancelarRelatorioSemestral(gerado.relatorioId)).then(() =>
                navigate('/portal/alunos'),
              );
            }}
          />
        </>
      )}
    </section>
  );
}

export default RelatorioSemestral;
