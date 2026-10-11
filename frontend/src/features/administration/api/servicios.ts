import { request } from '../../../shared/api/http';
import { mutate } from '../../identity/api/auth';

export interface Modalidad { id: string; nombre: string; conCargo: boolean; tarifa: number; activa: boolean }
export interface Servicio { id: string; nombre: string; duracionMinutos: number; activo: boolean; version: number; modalidades: Modalidad[] }
// La tarifa se envía como texto ("40.50") para que el servidor la reciba exacta, sin pasar por números de coma flotante.
export interface ModalidadSolicitud { id: string | null; nombre: string; conCargo: boolean; tarifa: string; activa: boolean }
export interface DatosServicio { nombre: string; duracionMinutos: number; modalidades: ModalidadSolicitud[] }

export const serviciosApi = {
  listar: (signal?: AbortSignal) => request<Servicio[]>('/api/servicios', { signal }),
  crear: (datos: DatosServicio) => mutate<Servicio>('/api/servicios', datos),
  actualizar: (servicio: Servicio, datos: DatosServicio & { activo: boolean }) =>
    mutate<Servicio>(`/api/servicios/${servicio.id}`, { ...datos, version: servicio.version }, undefined, 'PUT'),
};
