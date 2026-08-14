import { apiClient } from './apiClient';

/** POST /api/v1/professores (contracts/api-web-admin.md). */
export interface CadastrarProfessorRequest {
  nome: string;
  email: string;
}

export interface Professor {
  id: string;
  nome: string;
  email: string;
  codigoVinculacao: string;
  codigoVinculacaoExpiraEm: string;
}

/** Item de GET /api/v1/professores. */
export interface ProfessorListaItem {
  id: string;
  nome: string;
  email: string;
  vinculadoTelegram: boolean;
}

export async function cadastrarProfessor(dados: CadastrarProfessorRequest): Promise<Professor> {
  return apiClient.post<Professor>('/api/v1/professores', dados);
}

export async function listarProfessores(): Promise<ProfessorListaItem[]> {
  return apiClient.get<ProfessorListaItem[]>('/api/v1/professores');
}

export async function reemitirCodigoVinculacao(professorId: string): Promise<Professor> {
  return apiClient.post<Professor>(`/api/v1/professores/${professorId}/codigo-vinculacao`);
}
