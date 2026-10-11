import { request } from '../../../shared/api/http';
import { mutate, type Rol } from './auth';
export interface CuentaRoles { id: string; identificador: string; activa: boolean; roles: Rol[]; version: number; profesionalId?: string | null }
export interface Pagina<T> { elementos: T[]; hayMas: boolean }
export interface Cambio { id: string; actorId: string; anteriores: Rol[]; nuevos: Rol[]; fecha: string }
export const rolesApi = {
  listar: (busqueda: string, pagina: number, signal?: AbortSignal) => request<Pagina<CuentaRoles>>(`/api/cuentas?busqueda=${encodeURIComponent(busqueda)}&pagina=${pagina}`, { signal }),
  cargar: (id: string, signal?: AbortSignal) => request<CuentaRoles>(`/api/cuentas/${id}/roles`, { signal }),
  guardar: (cuenta: CuentaRoles, roles: Rol[]) => mutate<CuentaRoles>(`/api/cuentas/${cuenta.id}/roles`, { roles, version: cuenta.version }, undefined, 'PUT'),
  historial: (id: string, pagina: number, signal?: AbortSignal) => request<Pagina<Cambio>>(`/api/cuentas/${id}/roles/auditoria?pagina=${pagina}`, { signal }),
};
