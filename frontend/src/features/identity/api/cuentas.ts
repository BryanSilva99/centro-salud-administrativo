import { mutate, type Rol } from './auth';
import { request } from '../../../shared/api/http';
import type { CuentaRoles, Pagina } from './roles';
export interface CambioCuenta {
  id: string; actorId: string; tipo: string; identificadorAnterior: string | null;
  identificadorNuevo: string; activaAnterior: boolean | null; activaNueva: boolean; roles: Rol[]; fecha: string; profesionalAnterior?: string | null; profesionalNuevo?: string | null;
}
export const cuentasApi = {
  crear: (identificador: string, contrasena: string, roles: Rol[]) => mutate<CuentaRoles>('/api/cuentas', { identificador, contrasena, roles }),
  actualizar: (cuenta: CuentaRoles, identificador: string, activa: boolean) => mutate<CuentaRoles>(`/api/cuentas/${cuenta.id}`, { identificador, activa, version: cuenta.version }, undefined, 'PUT'),
  historial: (id: string, pagina: number, signal?: AbortSignal) => request<Pagina<CambioCuenta>>(`/api/cuentas/${id}/auditoria?pagina=${pagina}`, { signal }),
};
