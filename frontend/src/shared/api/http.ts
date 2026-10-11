export class ApiError extends Error {
  constructor(public readonly status: number, message: string, public readonly code?: string) {
    super(message);
    this.name = 'ApiError';
  }
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    const timeout = AbortSignal.timeout(15_000);
    const signal = options.signal ? AbortSignal.any([options.signal, timeout]) : timeout;
    response = await fetch(path, { ...options, signal, credentials: 'same-origin', cache: 'no-store' });
  } catch (error) {
    if (options.signal?.aborted) throw error;
    throw new ApiError(0, 'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.');
  }
  if (!response.ok) {
    const message = response.status === 401
      ? 'Identificador o contraseña incorrectos. Si ya lo intentaste varias veces, espera 15 minutos.'
      : response.status === 403
        ? 'La solicitud no pudo validarse. Vuelve a intentarlo.'
        : response.status === 400
          ? 'Revisa los datos e inténtalo de nuevo.'
          : 'El servicio no está disponible en este momento. Inténtalo de nuevo.';
    const problem = await response.json().catch(() => ({})) as { code?: string };
    throw new ApiError(response.status, message, problem.code);
  }
  if (response.status === 204) return undefined as T;
  try { return await response.json() as T; }
  catch { throw new ApiError(502, 'La respuesta del servidor no pudo procesarse. Inténtalo de nuevo.'); }
}

interface CsrfToken { headerName: string; token: string }

/** Todas las escrituras obtienen el token vigente, incluso las de features nuevas. */
export async function mutate<T>(path: string, body?: object, signal?: AbortSignal): Promise<T> {
  return mutateWithMethod<T>('POST', path, body, signal);
}

/** Igual que mutate, permitiendo verbos idempotentes para transiciones de estado. */
export async function mutateWithMethod<T>(method: string, path: string, body?: object, signal?: AbortSignal): Promise<T> {
  const csrf = await request<CsrfToken>('/api/auth/csrf', { signal });
  return request<T>(path, {
    method, signal,
    headers: { [csrf.headerName]: csrf.token, ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
}
