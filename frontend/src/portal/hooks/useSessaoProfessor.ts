import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import {
  descartarTokenPortal,
  guardarTokenPortal,
  obterTokenPortal,
  professorApiClient,
} from '../services/professorApiClient';

/**
 * Sessao do professor no portal (T058, FR-005).
 *
 * A renovacao do token e feita pelo `professorApiClient`, que grava o header `X-Refreshed-Token`
 * de toda resposta autenticada — ou seja, a sessao desliza a cada atividade real do professor. Este
 * hook cuida do estado da sessao na UI: valida o token guardado contra `GET /api/v1/professor/me`,
 * expoe o professor autenticado e permite encerrar a sessao descartando o token local (nao ha
 * invalidacao server-side no MVP, conforme data-model.md).
 */

export interface ProfessorSessao {
  id: string;
  nome: string;
  sessaoExpiraEm: string;
}

export type EstadoSessao = 'verificando' | 'autenticado' | 'expirado';

export interface UseSessaoProfessor {
  estado: EstadoSessao;
  professor: ProfessorSessao | null;
  autenticar: (token: string) => Promise<void>;
  encerrarSessao: () => void;
  revalidar: () => Promise<void>;
}

/**
 * Contexto que compartilha UMA unica sessao entre a guarda de rota e o layout. Sem ele, cada
 * componente que chamasse o hook dispararia a sua propria requisicao a `/api/v1/professor/me` a
 * cada navegacao. O provider vive em `components/SessaoProfessorProvider.tsx`.
 */
export const SessaoProfessorContext = createContext<UseSessaoProfessor | null>(null);

/** Estado da sessao compartilhado via contexto (usado pelo provider). */
export function useSessaoProfessorInterno(): UseSessaoProfessor {
  const [estado, setEstado] = useState<EstadoSessao>(() =>
    obterTokenPortal() ? 'verificando' : 'expirado',
  );
  const [professor, setProfessor] = useState<ProfessorSessao | null>(null);

  const revalidar = useCallback(async (): Promise<void> => {
    if (!obterTokenPortal()) {
      setProfessor(null);
      setEstado('expirado');
      return;
    }
    try {
      const dados = await professorApiClient.get<ProfessorSessao>('/api/v1/professor/me');
      setProfessor(dados);
      setEstado('autenticado');
    } catch {
      // Qualquer falha de validacao encerra a sessao: o cliente ja descartou o token em 401 e a
      // guarda de rota redireciona para `/portal/entrar`.
      descartarTokenPortal();
      setProfessor(null);
      setEstado('expirado');
    }
  }, []);

  useEffect(() => {
    void revalidar();
  }, [revalidar]);

  const autenticar = useCallback(
    async (token: string): Promise<void> => {
      guardarTokenPortal(token);
      setEstado('verificando');
      await revalidar();
    },
    [revalidar],
  );

  const encerrarSessao = useCallback((): void => {
    descartarTokenPortal();
    setProfessor(null);
    setEstado('expirado');
  }, []);

  return { estado, professor, autenticar, encerrarSessao, revalidar };
}

/** Acessa a sessao compartilhada pelo `SessaoProfessorProvider`. */
export function useSessaoProfessor(): UseSessaoProfessor {
  const contexto = useContext(SessaoProfessorContext);
  if (!contexto) {
    throw new Error('useSessaoProfessor exige que a arvore esteja dentro de SessaoProfessorProvider');
  }
  return contexto;
}
