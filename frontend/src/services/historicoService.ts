import { apiClient } from './apiClient';

/** Item de GET /api/v1/alunos/{id}/historico (contracts/api-web-admin.md, US4). */
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

export interface AlunoResumo {
  id: string;
  nome: string;
}

/** Resposta de GET /api/v1/alunos/{id}/historico. */
export interface HistoricoAluno {
  aluno: AlunoResumo;
  relatorios: ItemHistorico[];
}

export async function consultarHistorico(alunoId: string): Promise<HistoricoAluno> {
  return apiClient.get<HistoricoAluno>(`/api/v1/alunos/${alunoId}/historico`);
}
