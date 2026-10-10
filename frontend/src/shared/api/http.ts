export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
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
    throw new ApiError(response.status, message);
  }
  if (response.status === 204) return undefined as T;
  try { return await response.json() as T; }
  catch { throw new ApiError(502, 'La respuesta del servidor no pudo procesarse. Inténtalo de nuevo.'); }
}
