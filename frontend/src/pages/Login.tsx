import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError } from '../services/apiClient';
import { login } from '../services/authService';
import '../admin-theme.css';
import './login.css';

/** Tela de login do administrador (T068). */
function Login(): JSX.Element {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>): Promise<void> {
    event.preventDefault();
    setErro(null);
    setEnviando(true);
    try {
      await login({ email, senha });
      navigate('/historico');
    } catch (erroCapturado) {
      if (erroCapturado instanceof ApiError) {
        setErro(erroCapturado.message);
      } else {
        setErro('Nao foi possivel entrar. Tente novamente.');
      }
    } finally {
      setEnviando(false);
    }
  }

  return (
    <main className="adm adm-portao">
      <div className="adm-cartao adm-portao__cartao">
        <span className="adm-portao__area">Secretaria</span>
        <h1 className="adm-portao__marca">Escola de Música</h1>
        <p className="adm-portao__subtitulo">Entre para cadastrar professores, alunos e consultar relatórios.</p>

        <form onSubmit={handleSubmit}>
          {erro && (
            <p className="adm-alerta" role="alert">
              {erro}
            </p>
          )}

          <div className="adm-campo">
            <label className="adm-rotulo" htmlFor="email">
              E-mail
            </label>
            <input
              className="adm-entrada"
              id="email"
              name="email"
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
          </div>
          <div className="adm-campo">
            <label className="adm-rotulo" htmlFor="senha">
              Senha
            </label>
            <input
              className="adm-entrada"
              id="senha"
              name="senha"
              type="password"
              autoComplete="current-password"
              required
              value={senha}
              onChange={(event) => setSenha(event.target.value)}
            />
          </div>
          <button className="adm-botao adm-portao__botao" type="submit" disabled={enviando}>
            {enviando ? 'Entrando...' : 'Entrar'}
          </button>
        </form>

        {/*
          Esta tela e exclusiva da administracao. Professores entram pelo portal com o codigo de
          vinculacao (spec 002) — sem este ponto de entrada, um professor que chegasse aqui ficaria
          sem saber para onde ir, ja que nao tem e-mail/senha no sistema (FR-017a).
        */}
        <p className="adm-portao__desvio">
          É professor? <Link to="/portal/entrar">Acesse o portal do professor</Link>.
        </p>
      </div>
    </main>
  );
}

export default Login;
