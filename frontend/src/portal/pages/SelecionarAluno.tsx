import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AlunoResumo, listarAlunos } from '../services/professorPortalService';
import './selecionar-aluno.css';

/**
 * Selecao de aluno (T040), no estilo dos cards "Ongoing Calls" da imagem de referencia
 * (research.md secao 7): avatar com iniciais, nome, status e rodape com o identificador — com o
 * conteudo adaptado ao dominio (aluno e relatorios, nao chamadas telefonicas).
 */

function iniciais(nome: string): string {
  return nome
    .split(' ')
    .filter((parte) => parte.length > 0)
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join('');
}

function SelecionarAluno(): JSX.Element {
  const [alunos, setAlunos] = useState<AlunoResumo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [parametros] = useSearchParams();
  const busca = (parametros.get('busca') ?? '').toLowerCase();

  useEffect(() => {
    let ativo = true;
    listarAlunos()
      .then((lista) => {
        if (ativo) {
          setAlunos(lista);
        }
      })
      .catch((e: unknown) => {
        if (ativo) {
          setErro(e instanceof Error ? e.message : 'Não foi possível carregar seus alunos.');
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
  }, []);

  const visiveis = busca
    ? alunos.filter((aluno) => aluno.nome.toLowerCase().includes(busca))
    : alunos;

  if (carregando) {
    return (
      <p role="status" aria-live="polite">
        Carregando seus alunos...
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

  return (
    <section>
      <h1>Alunos</h1>
      <p>Escolha um aluno para registrar uma aula ou consultar o histórico.</p>

      {visiveis.length === 0 ? (
        <p style={{ marginTop: 24 }}>
          {busca
            ? `Nenhum aluno encontrado para "${parametros.get('busca')}".`
            : 'Você ainda não tem alunos associados. Fale com a secretaria da escola.'}
        </p>
      ) : (
        <ul className="aluno-grade">
          {visiveis.map((aluno) => (
            <li key={aluno.id} className="portal-card aluno-card">
              <div className="aluno-card__topo">
                <span className="portal-avatar" aria-hidden="true">
                  {iniciais(aluno.nome)}
                </span>
                <h3>{aluno.nome}</h3>
              </div>

              <div className="aluno-card__acoes">
                <Link className="portal-botao" to={`/portal/alunos/${aluno.id}/novo-relatorio`}>
                  Novo relatório de aula
                </Link>
                <Link
                  className="portal-botao portal-botao--secundario"
                  to={`/portal/alunos/${aluno.id}/relatorio-semestral`}
                >
                  Relatório semestral
                </Link>
                <Link
                  className="portal-botao portal-botao--secundario"
                  to={`/portal/alunos/${aluno.id}/historico`}
                >
                  Histórico
                </Link>
              </div>

              <footer className="aluno-card__rodape">ID {aluno.id.slice(0, 8)}</footer>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export default SelecionarAluno;
