import { Fragment, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../services/apiClient';
import {
  listarProfessores,
  Professor,
  ProfessorListaItem,
  reemitirCodigoVinculacao,
} from '../services/professorService';
import './professores.css';

/** Lista de professores cadastrados, com acao para (re)emitir o codigo de vinculacao. */
function Professores(): JSX.Element {
  const [professores, setProfessores] = useState<ProfessorListaItem[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [gerandoParaId, setGerandoParaId] = useState<string | null>(null);
  const [codigosGerados, setCodigosGerados] = useState<Record<string, Professor>>({});

  useEffect(() => {
    carregarProfessores();
  }, []);

  async function carregarProfessores(): Promise<void> {
    setCarregando(true);
    setErro(null);
    try {
      const lista = await listarProfessores();
      setProfessores(lista);
    } catch {
      setErro('Nao foi possivel carregar a lista de professores.');
    } finally {
      setCarregando(false);
    }
  }

  async function handleGerarCodigo(professorId: string): Promise<void> {
    setErro(null);
    setGerandoParaId(professorId);
    try {
      const professor = await reemitirCodigoVinculacao(professorId);
      setCodigosGerados((atual) => ({ ...atual, [professorId]: professor }));
    } catch (erroCapturado) {
      if (erroCapturado instanceof ApiError) {
        setErro(erroCapturado.message);
      } else {
        setErro('Nao foi possivel gerar o codigo de vinculacao. Tente novamente.');
      }
    } finally {
      setGerandoParaId(null);
    }
  }

  return (
    <section>
      <div className="adm-professores__cabecalho">
        <h1>Professores</h1>
        <Link className="adm-botao adm-botao--secundario" to="/professores/novo">
          Cadastrar professor
        </Link>
      </div>
      <p>Gere um novo código de vinculação para um professor vincular o Telegram ou logar no portal.</p>

      {erro && (
        <p className="adm-alerta" role="alert">
          {erro}
        </p>
      )}

      {carregando ? (
        <p role="status" aria-live="polite">
          Carregando professores...
        </p>
      ) : professores.length === 0 ? (
        <p className="adm-vazio">Nenhum professor cadastrado ainda.</p>
      ) : (
        <div className="adm-cartao adm-professores__tabela-wrapper">
          <table className="adm-professores__tabela">
            <thead>
              <tr>
                <th scope="col">Nome</th>
                <th scope="col">E-mail</th>
                <th scope="col">Telegram</th>
                <th scope="col">Ação</th>
              </tr>
            </thead>
            <tbody>
              {professores.map((professor) => {
                const codigoGerado = codigosGerados[professor.id];
                return (
                  <Fragment key={professor.id}>
                    <tr>
                      <td>{professor.nome}</td>
                      <td>{professor.email}</td>
                      <td>
                        <span className="adm-etiqueta">
                          {professor.vinculadoTelegram ? 'Vinculado' : 'Não vinculado'}
                        </span>
                      </td>
                      <td>
                        <button
                          type="button"
                          className="adm-botao adm-botao--secundario"
                          disabled={gerandoParaId === professor.id}
                          onClick={() => void handleGerarCodigo(professor.id)}
                        >
                          {gerandoParaId === professor.id ? 'Gerando...' : 'Gerar novo código'}
                        </button>
                      </td>
                    </tr>
                    {codigoGerado && (
                      <tr className="adm-professores__linha-codigo">
                        <td colSpan={4}>
                          <div className="adm-professores__codigo">
                            <span className="adm-professores__codigo-valor">
                              {codigoGerado.codigoVinculacao}
                            </span>
                            <span className="adm-professores__codigo-validade">
                              Válido até{' '}
                              {new Date(codigoGerado.codigoVinculacaoExpiraEm).toLocaleString(
                                'pt-BR',
                              )}
                            </span>
                          </div>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

export default Professores;
