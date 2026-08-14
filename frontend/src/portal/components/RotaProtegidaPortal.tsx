import { Navigate, Outlet } from 'react-router-dom';
import { useSessaoProfessor } from '../hooks/useSessaoProfessor';

/**
 * Guarda de rota do portal do professor (T020): valida a sessao contra
 * `GET /api/v1/professor/me` e redireciona para `/portal/entrar` quando ela expira (FR-005).
 *
 * Diferente de `RotaProtegida` (admin), que se baseia apenas na expiracao guardada localmente, aqui
 * a validacao passa pelo backend — a sessao do portal e deslizante, entao o token local nao carrega
 * sozinho a verdade sobre sua validade.
 */
function RotaProtegidaPortal(): JSX.Element {
  const { estado } = useSessaoProfessor();

  if (estado === 'verificando') {
    return (
      <div className="portal" role="status" aria-live="polite">
        <p style={{ padding: 24 }}>Verificando sua sessão...</p>
      </div>
    );
  }

  if (estado === 'expirado') {
    return <Navigate to="/portal/entrar" replace />;
  }

  return <Outlet />;
}

export default RotaProtegidaPortal;
