import { ReactNode } from 'react';
import { SessaoProfessorContext, useSessaoProfessorInterno } from '../hooks/useSessaoProfessor';

/**
 * Disponibiliza uma unica sessao de professor para toda a arvore `/portal/**` (guarda de rota,
 * layout e paginas), evitando que cada consumidor de `useSessaoProfessor` dispare a sua propria
 * validacao em `GET /api/v1/professor/me`.
 */
function SessaoProfessorProvider({ children }: { children: ReactNode }): JSX.Element {
  const sessao = useSessaoProfessorInterno();
  return (
    <SessaoProfessorContext.Provider value={sessao}>{children}</SessaoProfessorContext.Provider>
  );
}

export default SessaoProfessorProvider;
