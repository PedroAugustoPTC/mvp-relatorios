import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { logout } from '../services/authService';
import '../admin-theme.css';

/**
 * Shell visual da secretaria (T099), com a mesma composicao do portal do professor (spec 002):
 * painel arredondado sobre fundo escuro, trilha estreita de icones a esquerda com o item ativo em
 * roxo, e header com a marca da escola e o bloco de quem esta logado a direita.
 *
 * O conteudo e o da secretaria (cadastros e historico) — so a linguagem visual e compartilhada.
 *
 * Responsivo: em telas estreitas a trilha colapsa para uma barra horizontal no topo, igual ao
 * portal, para que a secretaria tambem funcione em tablet.
 */

interface ItemNavegacao {
  rota: string;
  rotulo: string;
  icone: string;
}

const ITENS_NAVEGACAO: ItemNavegacao[] = [
  { rota: '/professores/novo', rotulo: 'Professores', icone: '♪' },
  { rota: '/alunos/novo', rotulo: 'Alunos', icone: '☰' },
  { rota: '/historico', rotulo: 'Histórico', icone: '◷' },
];

function AdminLayout(): JSX.Element {
  const navigate = useNavigate();

  function encerrarSessao(): void {
    logout();
    navigate('/login');
  }

  return (
    <div className="adm">
      <div className="adm-shell">
        <nav className="adm-sidebar" aria-label="Seções da secretaria">
          <ul className="adm-sidebar__itens">
            {ITENS_NAVEGACAO.map((item) => (
              <li key={item.rota}>
                <NavLink
                  to={item.rota}
                  className={({ isActive }) => `adm-sidebar__item${isActive ? ' ativo' : ''}`}
                  aria-label={item.rotulo}
                  title={item.rotulo}
                >
                  <span aria-hidden="true">{item.icone}</span>
                </NavLink>
              </li>
            ))}
          </ul>

          <button
            type="button"
            className="adm-sidebar__item adm-sidebar__sair"
            aria-label="Sair"
            title="Sair"
            onClick={encerrarSessao}
          >
            <span aria-hidden="true">⏻</span>
          </button>
        </nav>

        <div className="adm-conteudo">
          <header className="adm-header">
            <span className="adm-header__marca">Escola de Música</span>

            <div className="adm-header__usuario">
              <div className="adm-header__usuario-dados">
                <strong>Secretaria</strong>
                <span>Administração</span>
              </div>
              <span className="adm-avatar" aria-hidden="true">
                SE
              </span>
            </div>
          </header>

          <main className="adm-main">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}

export default AdminLayout;
