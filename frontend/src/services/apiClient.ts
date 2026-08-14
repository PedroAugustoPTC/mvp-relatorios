/**
 * Cliente HTTP fino compartilhado por todos os services (T068-T070). Injeta o JWT do
 * administrador (quando presente) e converte o corpo de erro padrao do backend
 * ({ codigo, mensagem }, ver contracts/api-web-admin.md) em uma ApiError tipada.
 */

const API_BASE_URL: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080';

const TOKEN_STORAGE_KEY = 'escolaMusica.authToken';

export class ApiError extends Error {
  readonly codigo: string;
  readonly status: number;

  constructor(codigo: string, mensagem: string, status: number) {
    super(mensagem);
    this.name = 'ApiError';
    this.codigo = codigo;
    this.status = status;
  }
}

function obterToken(): string | null {
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = obterToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });

  if (!response.ok) {
    let codigo = 'ERRO_DESCONHECIDO';
    let mensagem = 'Ocorreu um erro inesperado. Tente novamente.';
    try {
      const corpo = (await response.json()) as { codigo?: string; mensagem?: string };
      codigo = corpo.codigo ?? codigo;
      mensagem = corpo.mensagem ?? mensagem;
    } catch {
      // Corpo vazio ou nao-JSON: mantem mensagem padrao.
    }
    throw new ApiError(codigo, mensagem, response.status);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const apiClient = {
  get: <T>(path: string): Promise<T> => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, body?: unknown): Promise<T> =>
    request<T>(path, {
      method: 'POST',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }),
};

export const AUTH_TOKEN_STORAGE_KEY = TOKEN_STORAGE_KEY;
