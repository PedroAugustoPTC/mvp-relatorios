import { useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import '../portal-theme.css';
import './portal-layout.css';
import { useSessaoProfessor } from '../hooks/useSessaoProfessor';

/**
 * Shell visual do portal do professor (T019), replicando a composicao da imagem de referencia
 * obrigatoria `materiais_apoio/Captura de tela 2026-08-14 084159.png` (FR-024, research.md secao 7):
 * painel externo arredondado sobre fundo quase preto, sidebar estreita de icones a esquerda com o
 * item ativo destacado em roxo, e header com identificacao da escola, busca em pill e bloco do
 * usuario logado a direita.
 *
 * O conteudo e adaptado ao dominio (professores/alunos/relatorios) — nunca o dominio textual do
 * modelo (central de atendimento), conforme as Assumptions do spec.
 *
 * Responsivo: em telas estreitas a sidebar colapsa para uma barra horizontal no topo, porque gravar
 * audio pelo celular e um caso de uso previsto.
 */

interface ItemNavegacao {
  rota: string;
  rotulo: string;
  icone: string;
}

const ITENS_NAVEGACAO: ItemNavegacao[] = [
  { rota: '/portal', rotulo: 'Painel', icone: '◎' },
  { rota: '/portal/alunos', rotulo: 'Alunos', icone: '☰' },
];

function iniciaisDe(nome: string | undefined): string {
  if (!nome) {
    return '–';
  }
  return nome
    .split(' ')
    .filter((parte) => parte.length > 0)
    .slice(0, 2)
    .map((parte) => parte[0]?.toUpperCase() ?? '')
    .join('');
}

function PortalLayout(): JSX.Element {
  const navigate = useNavigate();
  const location = useLocation();
  const { professor, encerrarSessao } = useSessaoProfessor();
  const [busca, setBusca] = useState('');

  const rotaAtiva = (rota: string): boolean =>
    rota === '/portal' ? location.pathname === '/portal' : location.pathname.startsWith(rota);

  return (
    <div className="portal">
      <div className="portal-shell">
        <nav className="portal-sidebar" aria-label="Navegação principal do portal">
          <ul className="portal-sidebar__itens">
            {ITENS_NAVEGACAO.map((item) => (
              <li key={item.rota}>
                <button
                  type="button"
                  className={`portal-sidebar__item${rotaAtiva(item.rota) ? ' ativo' : ''}`}
                  aria-current={rotaAtiva(item.rota) ? 'page' : undefined}
                  aria-label={item.rotulo}
                  title={item.rotulo}
                  onClick={() => navigate(item.rota)}
                >
                  <span aria-hidden="true">{item.icone}</span>
                </button>
              </li>
            ))}
          </ul>

          <button
            type="button"
            className="portal-sidebar__item portal-sidebar__sair"
            aria-label="Encerrar sessão"
            title="Encerrar sessão"
            onClick={() => {
              encerrarSessao();
              navigate('/portal/entrar', { replace: true });
            }}
          >
            <span aria-hidden="true">⏻</span>
          </button>
        </nav>

        <div className="portal-conteudo">
          <header className="portal-header">
            <span className="portal-header__marca">Portal do Professor</span>

            {/*
              A busca navega no submit (Enter / botao), nao a cada tecla: navegar a cada caractere
              reescreve a rota continuamente, o que atrapalha o historico do navegador e faz o
              leitor de tela anunciar a pagina repetidamente a cada letra digitada.
            */}
            <form
              className="portal-header__busca"
              role="search"
              onSubmit={(evento) => {
                evento.preventDefault();
                const termo = busca.trim();
                navigate(
                  termo ? `/portal/alunos?busca=${encodeURIComponent(termo)}` : '/portal/alunos',
                );
              }}
            >
              <label htmlFor="portal-busca-aluno" className="portal-visualmente-oculto">
                Buscar aluno
              </label>
              <input
                id="portal-busca-aluno"
                type="search"
                className="portal-campo portal-header__busca-campo"
                placeholder="Buscar aluno..."
                value={busca}
                onChange={(evento) => setBusca(evento.target.value)}
              />
              <button type="submit" className="portal-visualmente-oculto">
                Buscar
              </button>
            </form>

            <div className="portal-header__usuario">
              <div className="portal-header__usuario-dados">
                <strong>{professor?.nome ?? 'Carregando...'}</strong>
                <span>Professor</span>
              </div>
              <span className="portal-avatar" aria-hidden="true">
                {iniciaisDe(professor?.nome)}
              </span>
            </div>
          </header>

          <main className="portal-main">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}

export default PortalLayout;
