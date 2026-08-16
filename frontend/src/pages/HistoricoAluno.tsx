import { ChangeEvent, useEffect, useState } from 'react';
import PdfViewer from '../components/PdfViewer';
import { apiClient, ApiError } from '../services/apiClient';
import { Aluno, listarAlunos } from '../services/alunoService';
import { consultarHistorico, HistoricoAluno as HistoricoAlunoDto, ItemHistorico } from '../services/historicoService';
import './historico-aluno.css';

function rotuloItem(item: ItemHistorico): string {
  if (item.tipo === 'AULA') {
    return `Relatorio de aula - ${item.dataAula ?? 'data desconhecida'}`;
  }
  return `Relatorio semestral - ${item.periodoInicio ?? '?'} a ${item.periodoFim ?? '?'}`;
}

/** Consulta do historico de relatorios (aula + semestrais aprovados) de um aluno (T091, FR-017). */
function HistoricoAluno(): JSX.Element {
  const [alunos, setAlunos] = useState<Aluno[]>([]);
  const [alunoId, setAlunoId] = useState('');
  const [historico, setHistorico] = useState<HistoricoAlunoDto | null>(null);
  const [itemSelecionado, setItemSelecionado] = useState<ItemHistorico | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);

  useEffect(() => {
    listarAlunos()
      .then(setAlunos)
      .catch(() => setAlunos([]));
  }, []);

  async function handleAlunoChange(event: ChangeEvent<HTMLSelectElement>): Promise<void> {
    const idSelecionado = event.target.value;
    setAlunoId(idSelecionado);
    setHistorico(null);
    setItemSelecionado(null);
    setErro(null);

    if (!idSelecionado) {
      return;
    }

    setCarregando(true);
    try {
      const resultado = await consultarHistorico(idSelecionado);
      setHistorico(resultado);
    } catch (erroCapturado) {
      if (erroCapturado instanceof ApiError) {
        setErro(erroCapturado.message);
      } else {
        setErro('Nao foi possivel carregar o historico. Tente novamente.');
      }
    } finally {
      setCarregando(false);
    }
  }

  return (
    <section>
      <h1>Historico do aluno</h1>
      <p>Escolha um aluno para ver os relatórios já aprovados.</p>

      <div className="adm-campo adm-historico__filtro">
        <label className="adm-rotulo" htmlFor="alunoId">
          Aluno
        </label>
        <select
          className="adm-entrada"
          id="alunoId"
          name="alunoId"
          value={alunoId}
          onChange={handleAlunoChange}
        >
          <option value="">Selecione um aluno</option>
          {alunos.map((aluno) => (
            <option key={aluno.id} value={aluno.id}>
              {aluno.nome}
            </option>
          ))}
        </select>
      </div>

      {carregando && (
        <p role="status" aria-live="polite">
          Carregando historico...
        </p>
      )}
      {erro && (
        <p className="adm-alerta" role="alert">
          {erro}
        </p>
      )}

      {historico && (
        <div className="adm-historico">
          <section aria-label="Lista de relatorios">
            <h2 className="adm-historico__titulo">Relatorios de {historico.aluno.nome}</h2>
            {historico.relatorios.length === 0 ? (
              <p className="adm-vazio">Nenhum relatorio aprovado encontrado para este aluno.</p>
            ) : (
              <ul className="adm-indice">
                {historico.relatorios.map((item) => {
                  const selecionado = itemSelecionado?.id === item.id;
                  return (
                    <li key={`${item.tipo}-${item.id}`} className="adm-indice__item">
                      <button
                        type="button"
                        className={`adm-indice__botao${selecionado ? ' ativo' : ''}`}
                        aria-current={selecionado}
                        onClick={() => setItemSelecionado(item)}
                      >
                        <span className="adm-indice__rotulo">{rotuloItem(item)}</span>
                        <span className="adm-indice__badge">{item.status}</span>
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </section>

          {itemSelecionado ? (
            <section className="adm-cartao adm-ficha" aria-label="Detalhe do relatorio">
              <h2 className="adm-historico__titulo">Detalhe</h2>
              <dl className="adm-ficha__dados">
                <div className="adm-ficha__par">
                  <dt>Tipo</dt>
                  <dd>{itemSelecionado.tipo}</dd>
                </div>
                <div className="adm-ficha__par">
                  <dt>Status</dt>
                  <dd>{itemSelecionado.status}</dd>
                </div>
                {itemSelecionado.aprovadoEm && (
                  <div className="adm-ficha__par">
                    <dt>Aprovado em</dt>
                    <dd>{new Date(itemSelecionado.aprovadoEm).toLocaleString('pt-BR')}</dd>
                  </div>
                )}
              </dl>
              {itemSelecionado.pdfUrl ? (
                <PdfViewer
                  pdfUrl={itemSelecionado.pdfUrl}
                  titulo="Abrir PDF do relatorio"
                  baixarPdf={apiClient.baixarArquivo}
                />
              ) : (
                <p>PDF nao disponivel.</p>
              )}
            </section>
          ) : (
            historico.relatorios.length > 0 && (
              <p className="adm-vazio">Escolha um relatório da lista para abrir o PDF.</p>
            )
          )}
        </div>
      )}
    </section>
  );
}

export default HistoricoAluno;
