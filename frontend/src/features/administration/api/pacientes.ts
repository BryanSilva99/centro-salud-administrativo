import { request } from '../../../shared/api/http';
import { mutate } from '../../identity/api/auth';

export type TipoDocumento = 'DNI' | 'CE';
export interface NuevoPaciente {
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  fechaNacimiento: string;
  telefono: string | null;
  correo: string | null;
}
export interface Paciente extends NuevoPaciente { id: string; codigo: string; version: number }
export interface Pagina<T> { elementos: T[]; hayMas: boolean }

export const pacientesApi = {
  registrar: (datos: NuevoPaciente) => mutate<Paciente>('/api/pacientes', datos),
  buscar: (texto: string, pagina: number, signal?: AbortSignal) =>
    request<Pagina<Paciente>>(`/api/pacientes?busqueda=${encodeURIComponent(texto)}&pagina=${pagina}`, { signal }),
  actualizar: (paciente: Paciente, datos: NuevoPaciente) =>
    mutate<Paciente>(`/api/pacientes/${paciente.id}`, { ...datos, version: paciente.version }, undefined, 'PUT'),
};
