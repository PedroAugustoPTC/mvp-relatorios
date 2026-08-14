import { apiClient, AUTH_TOKEN_STORAGE_KEY } from './apiClient';

/** POST /api/v1/auth/login (contracts/api-web-admin.md). */
export interface LoginRequest {
  email: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
  expiraEm: string;
}

const EXPIRA_EM_STORAGE_KEY = 'escolaMusica.authTokenExpiraEm';

/** Autentica o administrador e persiste o token/expiracao em localStorage. */
export async function login(credenciais: LoginRequest): Promise<LoginResponse> {
  const resposta = await apiClient.post<LoginResponse>('/api/v1/auth/login', credenciais);
  localStorage.setItem(AUTH_TOKEN_STORAGE_KEY, resposta.token);
  localStorage.setItem(EXPIRA_EM_STORAGE_KEY, resposta.expiraEm);
  return resposta;
}

export function logout(): void {
  localStorage.removeItem(AUTH_TOKEN_STORAGE_KEY);
  localStorage.removeItem(EXPIRA_EM_STORAGE_KEY);
}

export function getToken(): string | null {
  return localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
}

/** Verifica presenca do token e se ainda nao expirou. */
export function isAuthenticated(): boolean {
  const token = getToken();
  const expiraEm = localStorage.getItem(EXPIRA_EM_STORAGE_KEY);
  if (!token || !expiraEm) {
    return false;
  }
  return new Date(expiraEm).getTime() > Date.now();
}
