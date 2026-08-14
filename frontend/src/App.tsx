import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import RotaProtegida from './components/RotaProtegida';
import CadastroAluno from './pages/CadastroAluno';
import CadastroProfessor from './pages/CadastroProfessor';
import HistoricoAluno from './pages/HistoricoAluno';
import Login from './pages/Login';

/** Roteamento da interface administrativa (T099). Professor nunca autentica aqui (FR-017a). */
function App(): JSX.Element {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route element={<RotaProtegida />}>
          <Route path="/professores/novo" element={<CadastroProfessor />} />
          <Route path="/alunos/novo" element={<CadastroAluno />} />
          <Route path="/historico" element={<HistoricoAluno />} />
          <Route path="/" element={<Navigate to="/historico" replace />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
