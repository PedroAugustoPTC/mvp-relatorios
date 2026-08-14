import { FormEvent, useState } from 'react';
import { ApiError } from '../services/apiClient';
import { cadastrarProfessor, Professor } from '../services/professorService';
import './cadastro-professor.css';

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
    <section>
      <h1>Cadastrar professor</h1>
      <p>O código de vinculação aparece aqui assim que o professor for cadastrado.</p>

      <form className="adm-cartao adm-formulario adm-cadastro" onSubmit={handleSubmit}>
        {erro && (
          <p className="adm-alerta" role="alert">
            {erro}
          </p>
        )}

        <div className="adm-campo">
          <label className="adm-rotulo" htmlFor="nome">
            Nome
          </label>
          <input
            className="adm-entrada"
            id="nome"
            name="nome"
            type="text"
            required
            value={nome}
            onChange={(event) => setNome(event.target.value)}
          />
        </div>
        <div className="adm-campo">
          <label className="adm-rotulo" htmlFor="email">
            E-mail
          </label>
          <input
            className="adm-entrada"
            id="email"
            name="email"
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </div>
        <button className="adm-botao" type="submit" disabled={enviando}>
          {enviando ? 'Cadastrando...' : 'Cadastrar'}
        </button>
      </form>

      {professorCriado && (
        <section className="adm-entrega" aria-label="Codigo de vinculacao gerado">
          <h2>Professor cadastrado com sucesso</h2>
          <p className="adm-entrega__rotulo">Código de vinculação</p>
          <p className="adm-entrega__codigo">{professorCriado.codigoVinculacao}</p>
          <p className="adm-entrega__validade">
            Válido até {new Date(professorCriado.codigoVinculacaoExpiraEm).toLocaleString('pt-BR')}
          </p>
          <p className="adm-entrega__instrucao">
            Repasse este código ao professor para que ele vincule sua conta do Telegram pelo bot.
          </p>
        </section>
      )}
    </section>
  );
}

export default CadastroProfessor;
