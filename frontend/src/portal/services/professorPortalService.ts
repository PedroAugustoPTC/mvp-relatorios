import { professorApiClient } from './professorApiClient';

/**
 * Consultas do portal sobre os alunos do professor autenticado (T039).
 * Contrato: `contracts/portal-web-api.md`, secao "Alunos".
 */

export interface AlunoResumo {
  id: string;
  nome: string;
}

export interface RascunhoPendente {
  existeRascunho: boolean;
  tipo: 'AULA' | 'SEMESTRAL' | null;
  relatorioId: string | null;
  status: string | null;
  canalOrigem: 'TELEGRAM' | 'WEB' | null;
  atualizadoEm: string | null;
}

export interface ItemHistorico {
  tipo: 'AULA' | 'SEMESTRAL';
  id: string;
  dataAula: string | null;
  periodoInicio: string | null;
  periodoFim: string | null;
  status: string;
  pdfUrl: string | null;
  aprovadoEm: string | null;
}

export interface HistoricoAluno {
  aluno: AlunoResumo;
  relatorios: ItemHistorico[];
}

export function listarAlunos(): Promise<AlunoResumo[]> {
  return professorApiClient.get<AlunoResumo[]>('/api/v1/professor/alunos');
}

/** Rascunho em aberto do aluno em qualquer canal, para oferecer retomada (FR-022a). */
export function consultarRascunhoPendente(alunoId: string): Promise<RascunhoPendente> {
  return professorApiClient.get<RascunhoPendente>(
    `/api/v1/professor/alunos/${alunoId}/rascunho-pendente`,
  );
}

/** Historico unificado (aula + semestral, qualquer canal) — FR-023. */
export function consultarHistorico(alunoId: string): Promise<HistoricoAluno> {
  return professorApiClient.get<HistoricoAluno>(
    `/api/v1/professor/alunos/${alunoId}/historico`,
  );
}
