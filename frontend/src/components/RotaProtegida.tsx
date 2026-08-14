import { Navigate, Outlet } from 'react-router-dom';
import { isAuthenticated } from '../services/authService';
import NavBar from './NavBar';

/** Exige administrador autenticado; redireciona ao login caso contrario (T099). */
function RotaProtegida(): JSX.Element {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  return (
    <>
      <NavBar />
      <Outlet />
    </>
  );
}

export default RotaProtegida;
