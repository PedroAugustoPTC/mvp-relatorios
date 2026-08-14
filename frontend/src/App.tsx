import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import RotaProtegida from './components/RotaProtegida';
import CadastroAluno from './pages/CadastroAluno';
import CadastroProfessor from './pages/CadastroProfessor';
import HistoricoAluno from './pages/HistoricoAluno';
import Login from './pages/Login';
import RotaProtegidaPortal from './portal/components/RotaProtegidaPortal';
import SessaoProfessorProvider from './portal/components/SessaoProfessorProvider';
import PortalLayout from './portal/layout/PortalLayout';
import HistoricoAlunoPortal from './portal/pages/HistoricoAlunoPortal';
import NovoRelatorioAula from './portal/pages/NovoRelatorioAula';
import PainelPortal from './portal/pages/PainelPortal';
import RelatorioSemestral from './portal/pages/RelatorioSemestral';
import RetomarRascunho from './portal/pages/RetomarRascunho';
import SelecionarAluno from './portal/pages/SelecionarAluno';
import VincularCodigo from './portal/pages/VincularCodigo';

/**
 * Roteamento da aplicacao.
 *
 * Duas areas independentes convivem aqui, com sessoes separadas (spec 002, Assumptions):
 *
 * <ul>
 *   <li><b>Interface administrativa</b> (`/login`, `/professores`, `/alunos`, `/historico`) — T099,
 *       spec 001. O professor nunca autentica aqui (FR-017a).
 *   <li><b>Portal do professor</b> (`/portal/**`) — spec 002. Autenticacao por codigo de
 *       vinculacao, sessao deslizante propria e identidade visual propria.
 * </ul>
 */
function App(): JSX.Element {
  return (
    <BrowserRouter>
      <Routes>
        {/* Interface administrativa (spec 001) */}
        <Route path="/login" element={<Login />} />
        <Route element={<RotaProtegida />}>
          <Route path="/professores/novo" element={<CadastroProfessor />} />
          <Route path="/alunos/novo" element={<CadastroAluno />} />
          <Route path="/historico" element={<HistoricoAluno />} />
          <Route path="/" element={<Navigate to="/historico" replace />} />
        </Route>

        {/* Portal do professor (spec 002) */}
        <Route
          path="/portal/*"
          element={
            <SessaoProfessorProvider>
              <Routes>
                {/* Publica: e por aqui que a sessao nasce, entao fica fora da guarda de rota. */}
                <Route path="entrar" element={<VincularCodigo />} />
                <Route element={<RotaProtegidaPortal />}>
                  <Route element={<PortalLayout />}>
                    <Route index element={<PainelPortal />} />
                    <Route path="alunos" element={<SelecionarAluno />} />
                    <Route path="alunos/:alunoId/novo-relatorio" element={<NovoRelatorioAula />} />
                    <Route path="alunos/:alunoId/retomar-rascunho" element={<RetomarRascunho />} />
                    <Route
                      path="alunos/:alunoId/relatorio-semestral"
                      element={<RelatorioSemestral />}
                    />
                    <Route path="alunos/:alunoId/historico" element={<HistoricoAlunoPortal />} />
                  </Route>
                </Route>
              </Routes>
            </SessaoProfessorProvider>
          }
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
