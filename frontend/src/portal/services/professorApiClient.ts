/**
 * Cliente HTTP do portal do professor (T021). Segue o mesmo padrao do `services/apiClient.ts` da
 * interface administrativa (ApiError tipada a partir do corpo `{ codigo, mensagem }` do backend),
 * com duas diferencas exigidas pela spec 002:
 *
 * 1. Usa o token de sessao do PROFESSOR, guardado sob uma chave propria — o portal e a interface
 *    administrativa sao sessoes independentes e nunca compartilham token.
 * 2. Implementa o lado cliente da sessao deslizante (FR-005): toda resposta autenticada traz o
 *    header `X-Refreshed-Token` com um token novo, que substitui o armazenado. Assim a sessao so
 *    expira apos 12h de inatividade real.
 */

const API_BASE_URL: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080';

const PORTAL_TOKEN_STORAGE_KEY = 'escolaMusica.portalToken';

/** Header de resposta que carrega o token renovado (ver ProfessorJwtAuthenticationFilter). */
const HEADER_TOKEN_RENOVADO = 'X-Refreshed-Token';

export class ApiError extends Error {
  readonly codigo: string;
  readonly status: number;
  /** Presente no bloqueio temporario de login (429, FR-004a): segundos ate poder tentar de novo. */
  readonly retryAfterSeconds?: number;

  constructor(codigo: string, mensagem: string, status: number, retryAfterSeconds?: number) {
    super(mensagem);
    this.name = 'ApiError';
    this.codigo = codigo;
    this.status = status;
    this.retryAfterSeconds = retryAfterSeconds;
  }
}

export function obterTokenPortal(): string | null {
  return localStorage.getItem(PORTAL_TOKEN_STORAGE_KEY);
}

export function guardarTokenPortal(token: string): void {
  localStorage.setItem(PORTAL_TOKEN_STORAGE_KEY, token);
}

export function descartarTokenPortal(): void {
  localStorage.removeItem(PORTAL_TOKEN_STORAGE_KEY);
}

/** Persiste o token renovado devolvido pelo backend, quando presente. */
function aplicarTokenRenovado(response: Response): void {
  const renovado = response.headers.get(HEADER_TOKEN_RENOVADO);
  if (renovado) {
    guardarTokenPortal(renovado);
  }
}

async function lancarApiError(response: Response): Promise<never> {
  let codigo = 'ERRO_DESCONHECIDO';
  let mensagem = 'Ocorreu um erro inesperado. Tente novamente.';
  let retryAfterSeconds: number | undefined;
  try {
    const corpo = (await response.json()) as {
      codigo?: string;
      mensagem?: string;
      retryAfterSeconds?: number;
    };
    codigo = corpo.codigo ?? codigo;
    mensagem = corpo.mensagem ?? mensagem;
    retryAfterSeconds = corpo.retryAfterSeconds;
  } catch {
    // Corpo vazio ou nao-JSON: mantem mensagem padrao.
  }
  throw new ApiError(codigo, mensagem, response.status, retryAfterSeconds);
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = obterTokenPortal();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  aplicarTokenRenovado(response);

  if (!response.ok) {
    // Sessao expirada/invalida: descarta o token local para que a guarda de rota redirecione ao
    // login em vez de repetir chamadas que sempre falharao com 401.
    if (response.status === 401) {
      descartarTokenPortal();
    }
    return lancarApiError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const professorApiClient = {
  get: <T>(path: string): Promise<T> => request<T>(path, { method: 'GET' }),

  post: <T>(path: string, body?: unknown): Promise<T> =>
    request<T>(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }),

  /**
   * Envio multipart (upload/gravacao de audio). O `Content-Type` e deliberadamente omitido: o
   * proprio navegador o define junto com o `boundary` do FormData.
   */
  postFormData: <T>(path: string, formData: FormData): Promise<T> =>
    request<T>(path, { method: 'POST', body: formData }),
};

export const PORTAL_TOKEN_KEY = PORTAL_TOKEN_STORAGE_KEY;
export const HEADER_TOKEN_RENOVADO_PORTAL = HEADER_TOKEN_RENOVADO;
