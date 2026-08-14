import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSessaoProfessor } from '../hooks/useSessaoProfessor';
import { ApiError } from '../services/professorApiClient';
import { vincularPorCodigo } from '../services/professorAuthService';
import '../portal-theme.css';
import './vincular-codigo.css';

/**
 * Entrada do professor no portal pelo codigo de vinculacao (T059, FR-003/FR-004/FR-004a).
 *
 * As mensagens de erro sao especificas e dizem o que fazer a seguir — codigo invalido manda pedir
 * um novo a secretaria; bloqueio temporario informa quanto tempo esperar (research.md secao 8). Um
 * "erro ao entrar" generico deixaria o professor sem saida.
 */

function minutosDe(segundos: number): string {
  const minutos = Math.ceil(segundos / 60);
  return minutos <= 1 ? '1 minuto' : `${minutos} minutos`;
}

function mensagemDeErro(erro: unknown): string {
  if (erro instanceof ApiError) {
    if (erro.codigo === 'CODIGO_INVALIDO') {
      return 'Código inválido ou expirado. Peça um novo código à secretaria da escola.';
    }
    if (erro.codigo === 'BLOQUEADO_TEMPORARIAMENTE') {
      const espera = erro.retryAfterSeconds;
      return espera
        ? `Muitas tentativas incorretas. Tente novamente em ${minutosDe(espera)}.`
        : 'Muitas tentativas incorretas. Aguarde alguns minutos antes de tentar de novo.';
    }
    return erro.message;
  }
  return 'Não foi possível entrar agora. Verifique sua conexão e tente novamente.';
}

function VincularCodigo(): JSX.Element {
  const navigate = useNavigate();
  const { autenticar } = useSessaoProfessor();

  const [codigo, setCodigo] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function entrar(evento: React.FormEvent): Promise<void> {
    evento.preventDefault();
    setErro(null);
    setEnviando(true);
    try {
      const resposta = await vincularPorCodigo(codigo.trim());
      autenticar(resposta.token);
      navigate('/portal', { replace: true });
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="portal vincular-pagina">
      <main className="portal-card vincular-cartao">
        <h1>Portal do Professor</h1>
        <p>
          Informe o mesmo código de vinculação que você usa no Telegram para acessar seus alunos e
          relatórios.
        </p>

        <form onSubmit={(evento) => void entrar(evento)} style={{ marginTop: 20 }}>
          <label className="portal-rotulo" htmlFor="portal-codigo-vinculacao">
            Código de vinculação
          </label>
          <input
            id="portal-codigo-vinculacao"
            className="portal-campo"
            type="text"
            autoComplete="one-time-code"
            value={codigo}
            onChange={(evento) => setCodigo(evento.target.value)}
            disabled={enviando}
            required
          />

          <button
            type="submit"
            className="portal-botao"
            style={{ marginTop: 16, width: '100%' }}
            disabled={enviando || codigo.trim().length === 0}
          >
            {enviando ? 'Entrando...' : 'Entrar'}
          </button>
        </form>

        {erro && (
          <p className="portal-erro" role="alert" style={{ marginTop: 16 }}>
            {erro}
          </p>
        )}
      </main>
    </div>
  );
}

export default VincularCodigo;
