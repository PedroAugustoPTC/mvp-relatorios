import { ChangeEvent, useEffect, useState } from 'react';
import PdfViewer from '../components/PdfViewer';
import { ApiError } from '../services/apiClient';
import { Aluno, listarAlunos } from '../services/alunoService';
import { consultarHistorico, HistoricoAluno as HistoricoAlunoDto, ItemHistorico } from '../services/historicoService';

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
    <main>
      <h1>Historico do aluno</h1>
      <div>
        <label htmlFor="alunoId">Aluno</label>
        <select id="alunoId" name="alunoId" value={alunoId} onChange={handleAlunoChange}>
          <option value="">Selecione um aluno</option>
          {alunos.map((aluno) => (
            <option key={aluno.id} value={aluno.id}>
              {aluno.nome}
            </option>
          ))}
        </select>
      </div>

      {carregando && <p>Carregando historico...</p>}
      {erro && <p role="alert">{erro}</p>}

      {historico && (
        <section aria-label="Lista de relatorios">
          <h2>Relatorios de {historico.aluno.nome}</h2>
          {historico.relatorios.length === 0 ? (
            <p>Nenhum relatorio aprovado encontrado para este aluno.</p>
          ) : (
            <ul>
              {historico.relatorios.map((item) => (
                <li key={`${item.tipo}-${item.id}`}>
                  <button type="button" onClick={() => setItemSelecionado(item)}>
                    {rotuloItem(item)}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {itemSelecionado && (
        <section aria-label="Detalhe do relatorio">
          <h2>Detalhe</h2>
          <p>Tipo: {itemSelecionado.tipo}</p>
          <p>Status: {itemSelecionado.status}</p>
          {itemSelecionado.aprovadoEm && (
            <p>Aprovado em: {new Date(itemSelecionado.aprovadoEm).toLocaleString('pt-BR')}</p>
          )}
          {itemSelecionado.pdfUrl ? (
            <PdfViewer pdfUrl={itemSelecionado.pdfUrl} titulo="Abrir PDF do relatorio" />
          ) : (
            <p>PDF nao disponivel.</p>
          )}
        </section>
      )}
    </main>
  );
}

export default HistoricoAluno;
