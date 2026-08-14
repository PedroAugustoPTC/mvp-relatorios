import { NavLink, useNavigate } from 'react-router-dom';
import { logout } from '../services/authService';

/** Navegacao principal da interface administrativa (T099). */
function NavBar(): JSX.Element {
  const navigate = useNavigate();

  function handleLogout(): void {
    logout();
    navigate('/login');
  }

  return (
    <nav>
      <NavLink to="/professores/novo">Cadastrar professor</NavLink>
      <NavLink to="/alunos/novo">Cadastrar aluno</NavLink>
      <NavLink to="/historico">Historico</NavLink>
      <button type="button" onClick={handleLogout}>
        Sair
      </button>
    </nav>
  );
}

export default NavBar;
