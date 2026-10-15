import { ApiError, mutate, request } from '../../../shared/api/http';

export interface Cuenta {
  id: string;
  identificador: string;
  activa: boolean;
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
