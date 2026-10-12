import { request } from '../../../shared/api/http';
import { mutate } from '../../identity/api/auth';
export interface Cargo { id: string; citaId: string; pacienteId: string; importe: number; estado: string }
export interface Pago { id: string; cargoId: string; sesionCajaId: string; importe: number; medio: string; tipo: string; referencia: string | null; fechaRegistro: string }
export interface Saldo { cargo: Cargo; movimientos: Pago[]; totalPagado: number; totalReversiones: number; saldoPendiente: number }
export interface SesionCaja { id: string; cajeroId: string; fondoInicial: number; estado: string; fechaApertura: string; fechaCierre: string | null; efectivoDeclarado: number | null; diferencia: number | null; motivoDiferencia: string | null }
export interface Solicitud { id: string; operacionId: string; operacionTipo: 'CARGO' | 'PAGO'; solicitanteId: string; motivo: string; estado: string }
export interface Contexto { perfil: { cajero: boolean; administrador: boolean }; citas: { cita: { id: string; pacienteId: string; paciente: string; inicio: string; estado: string; modalidad: string; importe: number }; cargos: Saldo[] }[]; sesiones: SesionCaja[]; solicitudes: Solicitud[] }
export interface Cierre { sesion: SesionCaja; movimientos: Pago[]; efectivoEsperado: number; cobrosEfectivo: number; devolucionesEfectivo: number }
export interface Reporte { fechaInicio: string; fechaFin: string; totalCobros: number; totalDevoluciones: number; ingresosNetos: number; cobros: Pago[]; devoluciones: Pago[] }
const base = '/api/billing';
export const billingApi = {
 contexto: (fecha: string) => request<Contexto>(`${base}/contexto?fecha=${fecha}`),
 cargo: (citaId: string) => mutate<Cargo>(`${base}/cargos`, { citaId }),
 abrir: (fondoInicial: string, clave: string) => mutate<SesionCaja>(`${base}/sesiones`, { fondoInicial, clave }),
 pagar: (cargoId: string, sesionCajaId: string, importe: string, medio: string, referencia: string, clave: string) => mutate<Pago>(`${base}/pagos`, { cargoId, sesionCajaId, importe, medio, referencia, clave }),
 solicitar: (operacionId: string, tipo: string, motivo: string) => mutate<Solicitud>(`${base}/reversiones`, { operacionId, tipo, motivo }),
 resolver: (id: string, aprobar: boolean, referencia: string) => mutate<Solicitud>(`${base}/reversiones/${id}/resolver`, { aprobar, referencia }),
 cerrar: (id: string, efectivoDeclarado: string, motivoDiferencia: string) => mutate<Cierre>(`${base}/sesiones/${id}/cerrar`, { efectivoDeclarado, motivoDiferencia }),
 cierre: (id: string) => request<Cierre>(`${base}/sesiones/${id}`),
 reporte: (desde: string, hasta: string) => request<Reporte>(`${base}/reportes/economico?fechaInicio=${desde}&fechaFin=${hasta}`),
};
