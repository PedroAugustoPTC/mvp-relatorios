import { Navigate } from 'react-router-dom';
import { isAuthenticated } from '../services/authService';
import AdminLayout from './AdminLayout';

/** Exige administrador autenticado; redireciona ao login caso contrario (T099). */
function RotaProtegida(): JSX.Element {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  return <AdminLayout />;
}

export default RotaProtegida;
