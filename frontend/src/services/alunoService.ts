import { apiClient } from './apiClient';

/** POST /api/v1/alunos (contracts/api-web-admin.md). */
export interface CadastrarAlunoRequest {
  nome: string;
  dataNascimento: string;
  cpf: string;
  nomeResponsavel?: string;
  professorIds: string[];
}

export interface Aluno {
  id: string;
  nome: string;
  dataNascimento: string;
  nomeResponsavel: string | null;
  menorDeIdade: boolean;
}

export async function cadastrarAluno(dados: CadastrarAlunoRequest): Promise<Aluno> {
  return apiClient.post<Aluno>('/api/v1/alunos', dados);
}

export async function listarAlunos(professorId?: string): Promise<Aluno[]> {
  const query = professorId ? `?professorId=${encodeURIComponent(professorId)}` : '';
  return apiClient.get<Aluno[]>(`/api/v1/alunos${query}`);
}

/** Mesma regra de maioridade usada no backend (Aluno.isMenorDeIdadeEm), calculada no cliente. */
export function isMenorDeIdade(dataNascimento: string): boolean {
  if (!dataNascimento) {
    return false;
  }
  const nascimento = new Date(dataNascimento);
  if (Number.isNaN(nascimento.getTime())) {
    return false;
  }
  const hoje = new Date();
  let idade = hoje.getFullYear() - nascimento.getFullYear();
  const aindaNaoFezAniversarioEsteAno =
    hoje.getMonth() < nascimento.getMonth() ||
    (hoje.getMonth() === nascimento.getMonth() && hoje.getDate() < nascimento.getDate());
  if (aindaNaoFezAniversarioEsteAno) {
    idade -= 1;
  }
  return idade < 18;
}
