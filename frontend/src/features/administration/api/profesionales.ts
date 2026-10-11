import { request } from '../../../shared/api/http';
import { mutate } from '../../identity/api/auth';

export interface FichaProfesional { id: string; identificacion: string; nombre: string; funcion: string; activa: boolean; version: number; serviciosHabilitados: string[] }
export interface DatosFicha { identificacion: string; nombre: string; funcion: string; serviciosHabilitados: string[] }

export const profesionalesApi = {
  listar: (signal?: AbortSignal) => request<FichaProfesional[]>('/api/profesionales/fichas', { signal }),
  crear: (datos: DatosFicha) => mutate<FichaProfesional>('/api/profesionales/fichas', datos),
  actualizar: (ficha: FichaProfesional, datos: DatosFicha & { activa: boolean }) =>
    mutate<FichaProfesional>(`/api/profesionales/fichas/${ficha.id}`, { ...datos, version: ficha.version }, undefined, 'PUT'),
};
