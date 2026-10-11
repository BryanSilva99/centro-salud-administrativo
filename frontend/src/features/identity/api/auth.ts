import { ApiError, mutate, request } from '../../../shared/api/http';

export type Rol = 'REC' | 'CAJ' | 'ADM' | 'PRO';
export type Permiso = 'CUENTAS_CONSULTAR' | 'CUENTAS_GESTIONAR' | 'ROLES_GESTIONAR' | 'PACIENTES_GESTIONAR' | 'SERVICIOS_GESTIONAR' | 'CONSULTORIOS_GESTIONAR' | 'PROFESIONALES_GESTIONAR' | 'DISPONIBILIDAD_GESTIONAR';

export interface Cuenta {
  id: string;
  identificador: string;
  activa: boolean;
  roles: Rol[];
  permisos: Permiso[];
}

interface Csrf { headerName: string; token: string }

// Obtener el token antes de cada escritura evita reutilizarlo tras login/logout.
// No reintentamos automáticamente un login: cada intento cuenta en el servidor.
export async function mutate<T>(path: string, body?: object, signal?: AbortSignal, method = 'POST'): Promise<T> {
  const csrf = await request<Csrf>('/api/auth/csrf', { signal });
  return request<T>(path, {
    method, signal,
    headers: { [csrf.headerName]: csrf.token, ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
}

export const auth = {
  async me(signal?: AbortSignal): Promise<Cuenta | null> {
    try { return await request<Cuenta>('/api/auth/me', { signal }); }
    catch (error) {
      if (error instanceof ApiError && error.status === 401) return null;
      throw error;
    }
  },
  login: (identificador: string, contrasena: string) =>
    mutate<Cuenta>('/api/auth/login', { identificador, contrasena }),
  logout: () => mutate<void>('/api/auth/logout'),
  activity: (signal?: AbortSignal) => mutate<void>('/api/auth/activity', undefined, signal),
};
