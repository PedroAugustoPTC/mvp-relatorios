import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { consultarHistorico, HistoricoAluno } from '../services/professorPortalService';

/**
 * Historico unificado do aluno no portal (T047, FR-023): relatorios de aula e semestrais, criados
 * pelo Telegram ou pelo proprio portal, em uma unica lista cronologica.
 */

function formatarData(valor: string | null): string {
  return valor ? new Date(valor).toLocaleDateString('pt-BR') : '—';
}

function descricao(
  tipo: string,
  dataAula: string | null,
  periodoInicio: string | null,
  periodoFim: string | null,
): string {
  if (tipo === 'AULA') {
    return `Aula de ${formatarData(dataAula)}`;
  }
  return `Semestral (${formatarData(periodoInicio)} a ${formatarData(periodoFim)})`;
}

function HistoricoAlunoPortal(): JSX.Element {
  const { alunoId = '' } = useParams();
  const [historico, setHistorico] = useState<HistoricoAluno | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    let ativo = true;
    consultarHistorico(alunoId)
      .then((resultado) => {
        if (ativo) {
          setHistorico(resultado);
        }
      })
      .catch((e: unknown) => {
        if (ativo) {
          setErro(e instanceof Error ? e.message : 'Não foi possível carregar o histórico.');
        }
      })
      .finally(() => {
        if (ativo) {
          setCarregando(false);
        }
      });
    return () => {
      ativo = false;
    };
  }, [alunoId]);

  if (carregando) {
    return (
      <p role="status" aria-live="polite">
        Carregando o histórico...
      </p>
    );
  }

  if (erro) {
    return (
      <p className="portal-erro" role="alert">
        {erro}
      </p>
    );
  }

  const relatorios = historico?.relatorios ?? [];

  return (
    <section>
      <h1>Histórico de {historico?.aluno.nome}</h1>

      {relatorios.length === 0 ? (
        <p>Este aluno ainda não tem relatórios aprovados.</p>
      ) : (
        <div style={{ overflowX: 'auto', marginTop: 16 }}>
          <table className="portal-card" style={{ width: '100%', borderCollapse: 'collapse' }}>
            <caption className="portal-visualmente-oculto">
              Relatórios aprovados do aluno, de todos os canais
            </caption>
            <thead>
              <tr>
                <th scope="col" style={{ textAlign: 'left' }}>
                  Relatório
                </th>
                <th scope="col" style={{ textAlign: 'left' }}>
                  Aprovado em
                </th>
                <th scope="col" style={{ textAlign: 'left' }}>
                  PDF
                </th>
              </tr>
            </thead>
            <tbody>
              {relatorios.map((item) => (
                <tr key={item.id}>
                  <td>
                    {descricao(item.tipo, item.dataAula, item.periodoInicio, item.periodoFim)}
                  </td>
                  <td>{formatarData(item.aprovadoEm)}</td>
                  <td>
                    {item.pdfUrl ? (
                      <a href={item.pdfUrl} target="_blank" rel="noopener noreferrer">
                        Abrir PDF
                      </a>
                    ) : (
                      '—'
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default HistoricoAlunoPortal;
