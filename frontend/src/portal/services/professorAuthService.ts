import {
  descartarTokenPortal,
  guardarTokenPortal,
  obterTokenPortal,
  professorApiClient,
} from './professorApiClient';

/**
 * Autenticacao do professor no portal (T057). Contrato: `contracts/portal-web-api.md`, secao
 * "Autenticacao".
 *
 * A renovacao do token (header `X-Refreshed-Token`) e responsabilidade do `professorApiClient`, que
 * a aplica em toda resposta autenticada — aqui so tratamos o login inicial e o logout.
 */

export interface AutenticacaoProfessor {
  token: string;
  expiraEm: string;
  professor: { id: string; nome: string };
}

/** Autentica pelo codigo de vinculacao e ja persiste o token da sessao. */
export async function vincularPorCodigo(
  codigoVinculacao: string,
): Promise<AutenticacaoProfessor> {
  const resposta = await professorApiClient.post<AutenticacaoProfessor>(
    '/api/v1/professor/auth/vincular',
    { codigoVinculacao },
  );
  guardarTokenPortal(resposta.token);
  return resposta;
}

/**
 * Encerra a sessao descartando o token local. Nao ha invalidacao no servidor: a sessao e um JWT sem
 * estado, cuja validade se esgota por inatividade (data-model.md).
 */
export function sairDoPortal(): void {
  descartarTokenPortal();
}

export function tokenDoPortal(): string | null {
  return obterTokenPortal();
}
