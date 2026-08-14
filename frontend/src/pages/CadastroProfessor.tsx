import { FormEvent, useState } from 'react';
import { ApiError } from '../services/apiClient';
import { cadastrarProfessor, Professor } from '../services/professorService';

/** Cadastro de professor pela interface administrativa (T069, FR-001/FR-002). */
function CadastroProfessor(): JSX.Element {
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [professorCriado, setProfessorCriado] = useState<Professor | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setErro(null);
    setEnviando(true);
    try {
      const professor = await cadastrarProfessor({ nome, email });
      setProfessorCriado(professor);
      setNome('');
      setEmail('');
    } catch (erroCapturado) {
      if (erroCapturado instanceof ApiError) {
        setErro(erroCapturado.message);
      } else {
        setErro('Nao foi possivel cadastrar o professor. Tente novamente.');
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main>
      <h1>Cadastrar professor</h1>
      <form onSubmit={handleSubmit}>
        <div>
          <label htmlFor="nome">Nome</label>
          <input
            id="nome"
            name="nome"
            type="text"
            required
            value={nome}
            onChange={(event) => setNome(event.target.value)}
          />
        </div>
        <div>
          <label htmlFor="email">E-mail</label>
          <input
            id="email"
            name="email"
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </div>
        {erro && <p role="alert">{erro}</p>}
        <button type="submit" disabled={enviando}>
          {enviando ? 'Cadastrando...' : 'Cadastrar'}
        </button>
      </form>

      {professorCriado && (
        <section aria-label="Codigo de vinculacao gerado">
          <h2>Professor cadastrado com sucesso</h2>
          <p>
            Codigo de vinculacao: <strong>{professorCriado.codigoVinculacao}</strong>
          </p>
          <p>
            Valido ate:{' '}
            {new Date(professorCriado.codigoVinculacaoExpiraEm).toLocaleString('pt-BR')}
          </p>
          <p>
            Repasse este codigo ao professor para que ele vincule sua conta do Telegram pelo bot.
          </p>
        </section>
      )}
    </main>
  );
}

export default CadastroProfessor;
