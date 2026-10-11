import { request } from '../../../shared/api/http';
import { mutate } from '../../identity/api/auth';

export type TipoRecurso = 'profesionales' | 'consultorios';
export type TipoTurno = 'DISPONIBLE' | 'DESCANSO';
export interface Turno { id: string; fecha: string; horaInicio: string; horaFin: string; tipo: TipoTurno; activo: boolean }
export interface NuevoTurno { fecha: string; horaInicio: string; horaFin: string; tipo: TipoTurno }

export const disponibilidadApi = {
  listar: (tipo: TipoRecurso, recursoId: string, signal?: AbortSignal) =>
    request<Turno[]>(`/api/disponibilidad/${tipo}/${recursoId}`, { signal }),
  crear: (tipo: TipoRecurso, recursoId: string, datos: NuevoTurno) =>
    mutate<Turno>(`/api/disponibilidad/${tipo}/${recursoId}`, datos),
  desactivar: (tipo: TipoRecurso, recursoId: string, turnoId: string) =>
    mutate<void>(`/api/disponibilidad/${tipo}/${recursoId}/${turnoId}`, { activo: false }, undefined, 'PUT'),
};
