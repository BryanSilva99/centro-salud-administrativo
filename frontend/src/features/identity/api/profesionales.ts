import { request } from '../../../shared/api/http';
import { mutate } from './auth';
import type { CuentaRoles } from './roles';
export interface FichaProfesional { id: string; identificacion: string; nombre: string; funcion: string; activa: boolean }
export const profesionalesApi = {
  buscar: (texto: string, signal?: AbortSignal) => request<FichaProfesional[]>(`/api/profesionales/fichas?busqueda=${encodeURIComponent(texto)}`, { signal }),
  consultar: (id: string, signal?: AbortSignal) => request<FichaProfesional>(`/api/profesionales/fichas/${id}`, { signal }),
  registrar: (identificacion: string, nombre: string, funcion: string) => mutate<FichaProfesional>('/api/profesionales/fichas', { identificacion, nombre, funcion }),
  vincular: (cuenta: CuentaRoles, profesionalId: string | null) => mutate<CuentaRoles>(`/api/cuentas/${cuenta.id}/profesional`, { profesionalId, version: cuenta.version }, undefined, 'PUT'),
};
