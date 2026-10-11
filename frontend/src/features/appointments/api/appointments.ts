import { request } from '../../../shared/api/http';
import { mutate } from '../../identity/api/auth';
const mutateWithMethod = <T,>(method: string, path: string, body?: object, signal?: AbortSignal) => mutate<T>(path, body, signal, method);

export interface Paciente { id: string; nombre: string; activo: boolean }
export interface Profesional { id: string; nombre: string; especialidad: string; activo: boolean }
export interface Consultorio { id: string; nombre: string; activo: boolean }
export interface Modalidad { codigo: string; nombre: string; importe: number }
export interface Servicio { id: string; nombre: string; activo: boolean; duracionMinutos: number; modalidades: Modalidad[] }
export interface Contexto {
  pacientes: Paciente[]; profesionales: Profesional[]; servicios: Servicio[]; consultorios: Consultorio[];
  capacidades?: string[]; profesionalAsignadoId?: string | null;
}
export interface Cita {
  id: string; pacienteId: string; paciente: string; profesionalId: string; profesional: string; especialidad: string;
  servicioId: string; servicio: string; consultorioId: string; consultorio: string; inicio: string; fin: string;
  estado: string; duracionMinutos: number; modalidad: string; modalidadNombre: string; importe: number;
  llegadaEn?: string | null; atencionIniciadaEn?: string | null; atencionFinalizadaEn?: string | null;
  tipoCierre?: string | null; motivoCierre?: string | null; cerradaEn?: string | null; version: number;
}
export interface Horario { inicio: string; fin: string }
export interface Atencion { fecha: string; consultadaEn: string; enEspera: Cita[]; enAtencion: Cita[] }
export interface CierreResponse { cita: Cita; gestionEconomicaPendiente: boolean }
export interface Dashboard {
  fecha: string; profesionalId?: string | null; servicioId?: string | null; consultorioId?: string | null;
  actualizadoEn: string; porEstado: Record<string, number>; total: number;
  enEspera: number; atendidas: number; ausentes: number;
}

const params = (values: Record<string, string | undefined>) => {
  const query = new URLSearchParams();
  Object.entries(values).forEach(([key, value]) => { if (value) query.set(key, value); });
  return query.toString();
};

export const appointments = {
  contexto: (signal?: AbortSignal) => request<Contexto>('/api/citas/contexto', { signal }),
  agenda: (values: { fecha: string; profesionalId?: string; servicioId?: string; consultorioId?: string }, signal?: AbortSignal) =>
    request<Cita[]>(`/api/citas/agenda?${params(values)}`, { signal }),
  disponibilidad: (values: { fecha: string; pacienteId: string; profesionalId: string; servicioId: string; consultorioId: string; excluirCitaId?: string }, signal?: AbortSignal) =>
    request<Horario[]>(`/api/citas/disponibilidad?${params(values)}`, { signal }),
  programar: (body: { pacienteId: string; profesionalId: string; servicioId: string; consultorioId: string; modalidad: string; fecha: string; hora: string }) =>
    mutate<Cita>('/api/citas', body),
  reprogramar: (id: string, body: { fecha: string; hora: string; motivo: string; version: number }) =>
    mutate<Cita>(`/api/citas/${id}/reprogramaciones`, body),
  atencion: (values: { fecha: string; profesionalId?: string }, signal?: AbortSignal) =>
    request<Atencion>(`/api/citas/atencion?${params(values)}`, { signal }),
  llegada: (id: string, version: number) =>
    mutateWithMethod<Cita>('PUT', `/api/citas/${id}/llegada`, { version }),
  iniciarAtencion: (id: string, version: number) =>
    mutateWithMethod<Cita>('PUT', `/api/citas/${id}/atencion/inicio`, { version }),
  finalizarAtencion: (id: string, version: number) =>
    mutateWithMethod<Cita>('PUT', `/api/citas/${id}/atencion/finalizacion`, { version }),
  cancelar: (id: string, body: { motivo: string; version: number }) =>
    mutateWithMethod<CierreResponse>('PUT', `/api/citas/${id}/cancelacion`, body),
  ausencia: (id: string, version: number) =>
    mutateWithMethod<Cita>('PUT', `/api/citas/${id}/ausencia`, { version }),
  retirarEspera: (id: string, body: { motivo: string; version: number }) =>
    mutateWithMethod<CierreResponse>('PUT', `/api/citas/${id}/espera/retiro`, body),
  dashboard: (values: { fecha: string; profesionalId?: string; servicioId?: string; consultorioId?: string }, signal?: AbortSignal) =>
    request<Dashboard>(`/api/citas/dashboard?${params(values)}`, { signal }),
};
