import { request } from '../../../shared/api/http';

export interface Cargo {
  id: string;
  citaId: string;
  pacienteId: string;
  importe: string;
  estado: string;
}

export interface SesionCaja {
  id: string;
  cajeroId: string;
  fondoInicial: string;
  estado: string;
}

export interface Pago {
  id: string;
  cargoId: string;
  sesionCajaId: string;
  importe: string;
  medio: string;
  tipo: string;
}

export interface SaldoCargo {
  cargoId: string;
  importe: string;
  totalPagado: string;
  totalReversiones: string;
  saldoPendiente: string;
}

export interface SolicitudReversion {
  id: string;
  operacionId: string;
  estado: string;
}

export interface CierreCaja {
  sesionId: string;
  efectivoEsperado: string;
  efectivoDeclarado: string;
  diferencia: string;
}

export interface ReporteEconomico {
  fechaInicio: string;
  fechaFin: string;
  totalCobros: string;
  totalDevoluciones: string;
  ingresosNetos: string;
}

export interface RegistrarCargoRequest {
  citaId: string;
  pacienteId: string;
  importe: string;
}

export interface AbrirSesionRequest {
  cajeroId: string;
  fondoInicial: string;
}

export interface RegistrarPagoRequest {
  cargoId: string;
  sesionCajaId: string;
  importe: string;
  medio: string;
  referencia?: string;
}

export interface SolicitarReversionRequest {
  operacionId: string;
  tipo: 'CARGO' | 'PAGO';
  solicitanteId: string;
  motivo: string;
}

export interface ResolverReversionRequest {
  aprobadorId: string;
  aprobar: boolean;
}

export interface CerrarCajaRequest {
  efectivoDeclarado: string;
  motivoDiferencia?: string;
}

export const billingApi = {
  registrarCargo: (data: RegistrarCargoRequest) =>
    request<Cargo>('/api/billing/cargos', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),

  abrirSesion: (data: AbrirSesionRequest) =>
    request<SesionCaja>('/api/billing/sesiones', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),

  registrarPago: (data: RegistrarPagoRequest) =>
    request<Pago>('/api/billing/pagos', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),

  consultarSaldo: (cargoId: string) =>
    request<SaldoCargo>(`/api/billing/cargos/${cargoId}/saldo`),

  solicitarReversion: (data: SolicitarReversionRequest) =>
    request<SolicitudReversion>('/api/billing/reversiones', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),

  resolverReversion: (solicitudId: string, data: ResolverReversionRequest) =>
    request<SolicitudReversion>(`/api/billing/reversiones/${solicitudId}/resolver`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),

  cerrarCaja: (sesionId: string, data: CerrarCajaRequest) =>
    request<CierreCaja>(`/api/billing/sesiones/${sesionId}/cerrar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    }),

  reporteEconomico: (fechaInicio: string, fechaFin: string) =>
    request<ReporteEconomico>(
      `/api/billing/reportes/economico?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`
    ),
};
