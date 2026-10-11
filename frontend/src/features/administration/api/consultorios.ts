import { request } from '../../../shared/api/http';
import { mutate } from '../../identity/api/auth';

export interface Consultorio { id: string; codigo: string; nombre: string; activo: boolean; version: number; serviciosCompatibles: string[] }
export interface DatosConsultorio { codigo: string; nombre: string; serviciosCompatibles: string[] }

export const consultoriosApi = {
  listar: (signal?: AbortSignal) => request<Consultorio[]>('/api/consultorios', { signal }),
  crear: (datos: DatosConsultorio) => mutate<Consultorio>('/api/consultorios', datos),
  actualizar: (consultorio: Consultorio, datos: DatosConsultorio & { activo: boolean }) =>
    mutate<Consultorio>(`/api/consultorios/${consultorio.id}`, { ...datos, version: consultorio.version }, undefined, 'PUT'),
};
