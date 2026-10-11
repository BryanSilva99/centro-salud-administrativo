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
export interface Paciente extends NuevoPaciente { id: string; codigo: string }

export const pacientesApi = {
  registrar: (datos: NuevoPaciente) => mutate<Paciente>('/api/pacientes', datos),
};
