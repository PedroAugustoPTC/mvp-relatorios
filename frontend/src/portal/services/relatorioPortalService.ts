import { professorApiClient } from './professorApiClient';

/**
 * Fluxo de relatorio de aula pelo portal (T042). Contrato: `contracts/portal-web-api.md`, secao
 * "Relatorio de aula (portal)". As chamadas de relatorio semestral sao adicionadas em T070.
 */

export interface RelatorioAulaPortal {
  relatorioId: string;
  status: string;
  conteudosTrabalhados: string[];
  evolucao: string | null;
  dificuldades: string[];
  atividadesPropostas: string[];
  observacoes: string | null;
  perguntasPendentes: string[];
  pdfUrl: string | null;
  versao: number;
}

export interface EnvioAudioResultado {
  relatorioId: string;
  status: string;
  perguntasPendentes: string[];
}

export interface AprovacaoResultado {
  relatorioId: string;
  status: string;
  aprovadoEm: string;
}

/**
 * Envia o audio da aula (arquivo escolhido ou blob gravado pelo navegador). O `dataAula` e opcional
 * — o backend assume a data de hoje quando ausente.
 */
export function enviarAudio(
  alunoId: string,
  audio: Blob,
  nomeArquivo = 'aula.webm',
  dataAula?: string,
): Promise<EnvioAudioResultado> {
  const formData = new FormData();
  formData.append('audio', audio, nomeArquivo);
  if (dataAula) {
    formData.append('dataAula', dataAula);
  }
  return professorApiClient.postFormData<EnvioAudioResultado>(
    `/api/v1/professor/alunos/${alunoId}/relatorios-aula/audio`,
    formData,
  );
}

export function consultarRelatorioAula(relatorioId: string): Promise<RelatorioAulaPortal> {
  return professorApiClient.get<RelatorioAulaPortal>(
    `/api/v1/professor/relatorios-aula/${relatorioId}`,
  );
}

export function responderPergunta(
  relatorioId: string,
  resposta: string,
): Promise<RelatorioAulaPortal> {
  return professorApiClient.post<RelatorioAulaPortal>(
    `/api/v1/professor/relatorios-aula/${relatorioId}/responder-pergunta`,
    { resposta },
  );
}

export function revisarRelatorioAula(
  relatorioId: string,
  instrucao: string,
): Promise<RelatorioAulaPortal> {
  return professorApiClient.post<RelatorioAulaPortal>(
    `/api/v1/professor/relatorios-aula/${relatorioId}/revisar`,
    { instrucao },
  );
}

/**
 * Aprova a versao vigente. A `versaoConfirmada` e obrigatoria e o backend recusa (409) se ela nao
 * corresponder a versao atual — e o que impede aprovar um PDF que ja foi substituido por uma
 * revisao mais nova.
 */
export function aprovarRelatorioAula(
  relatorioId: string,
  versaoConfirmada: number,
): Promise<AprovacaoResultado> {
  return professorApiClient.post<AprovacaoResultado>(
    `/api/v1/professor/relatorios-aula/${relatorioId}/aprovar`,
    { versaoConfirmada },
  );
}

export function cancelarRelatorioAula(relatorioId: string): Promise<void> {
  return professorApiClient.post<void>(
    `/api/v1/professor/relatorios-aula/${relatorioId}/cancelar`,
  );
}

// ---------------------------------------------------------------------------------------------
// Relatorio semestral (T070) — contracts/portal-web-api.md, secao "Relatorio semestral (portal)"
// ---------------------------------------------------------------------------------------------

export interface SemestreDisponivel {
  chave: string;
  rotulo: string;
  inicio: string;
  fim: string;
}

export interface RelatorioSemestralPortal {
  relatorioId: string;
  status: string;
  quantidadeRelatoriosAulaConsiderados: number;
  parecerFinal: string | null;
  pdfUrl: string | null;
  versao: number;
}

export interface GeracaoSemestralResultado {
  quantidadeRelatoriosEncontrados: number;
  relatorioSemestralId: string;
  status: string;
  pdfUrl: string | null;
  versao: number;
}

export function listarSemestresDisponiveis(alunoId: string): Promise<SemestreDisponivel[]> {
  return professorApiClient.get<SemestreDisponivel[]>(
    `/api/v1/professor/alunos/${alunoId}/semestres-disponiveis`,
  );
}

/**
 * Consolida o semestre escolhido. Responde 409 `SEM_DADOS_NO_PERIODO` quando nenhum relatorio de
 * aula aprovado existe no periodo — nesse caso nada e criado (FR-019).
 */
export function gerarRelatorioSemestral(
  alunoId: string,
  semestre: string,
): Promise<GeracaoSemestralResultado> {
  return professorApiClient.post<GeracaoSemestralResultado>(
    `/api/v1/professor/alunos/${alunoId}/relatorios-semestrais`,
    { semestre },
  );
}

export function consultarRelatorioSemestral(
  relatorioId: string,
): Promise<RelatorioSemestralPortal> {
  return professorApiClient.get<RelatorioSemestralPortal>(
    `/api/v1/professor/relatorios-semestrais/${relatorioId}`,
  );
}

export function revisarRelatorioSemestral(
  relatorioId: string,
  instrucao: string,
): Promise<RelatorioSemestralPortal> {
  return professorApiClient.post<RelatorioSemestralPortal>(
    `/api/v1/professor/relatorios-semestrais/${relatorioId}/revisar`,
    { instrucao },
  );
}

export function aprovarRelatorioSemestral(
  relatorioId: string,
  versaoConfirmada: number,
): Promise<AprovacaoResultado> {
  return professorApiClient.post<AprovacaoResultado>(
    `/api/v1/professor/relatorios-semestrais/${relatorioId}/aprovar`,
    { versaoConfirmada },
  );
}

export function cancelarRelatorioSemestral(relatorioId: string): Promise<void> {
  return professorApiClient.post<void>(
    `/api/v1/professor/relatorios-semestrais/${relatorioId}/cancelar`,
  );
}
