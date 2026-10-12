import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { SessionView } from '../src/features/identity/components/SessionView';
import { BillingView } from '../src/features/billing/components/BillingView';
import { billingApi, type Contexto } from '../src/features/billing/api/billing';
import type { Cuenta } from '../src/features/identity/api/auth';
vi.mock('../src/features/identity/hooks/useSessionActivity', () => ({ useSessionActivity: vi.fn() }));
vi.mock('../src/features/billing/api/billing', () => ({ billingApi: { contexto: vi.fn(), cargo: vi.fn(), abrir: vi.fn(), pagar: vi.fn(), solicitar: vi.fn(), resolver: vi.fn(), cerrar: vi.fn(), cierre: vi.fn(), reporte: vi.fn() } }));
const cuenta: Cuenta = { id: 'cajero', identificador: 'caja', activa: true, roles: ['CAJ'], permisos: ['CAJA_CONSULTAR'] };
const contexto = (): Contexto => ({ perfil: { cajero: true, administrador: false }, citas: [{ cita: { id: 'cita', pacienteId: 'paciente', paciente: 'Paciente Sintético', inicio: '2026-10-12T14:00:00Z', estado: 'PROGRAMADA', modalidad: 'Particular', importe: 40 }, cargos: [] }], sesiones: [], solicitudes: [] });
beforeEach(() => { vi.clearAllMocks(); vi.mocked(billingApi.contexto).mockResolvedValue(contexto()); });
describe('Caja integrada', () => {
 it('genera cargo seleccionando la cita sin pedir paciente ni importe libres', async () => {
  render(<BillingView cuenta={cuenta} onExpired={vi.fn()} />);
  fireEvent.change(await screen.findByLabelText('Cita'), { target: { value: 'cita' } });
  fireEvent.click(screen.getByRole('button', { name: 'Generar cargo' }));
  await waitFor(() => expect(billingApi.cargo).toHaveBeenCalledWith('cita'));
  expect(screen.queryByLabelText('ID del paciente')).not.toBeInTheDocument();
 });
 it('ADM consulta y resuelve solicitudes ajenas sin recibir operaciones de cajero', async () => {
  const d = contexto(); d.perfil = { cajero: false, administrador: true };
  d.solicitudes = [{ id: 'sol', operacionId: 'pago', operacionTipo: 'PAGO', solicitanteId: 'otro', motivo: 'Devolver', estado: 'PENDIENTE' }];
  vi.mocked(billingApi.contexto).mockResolvedValue(d);
  render(<BillingView cuenta={{ ...cuenta, roles: ['ADM'] }} onExpired={vi.fn()} />);
  fireEvent.change(await screen.findByLabelText('Cita'), { target: { value: 'cita' } });
  expect(screen.queryByRole('button', { name: 'Abrir caja' })).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Generar cargo' })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Aprobar reversión' }));
  await waitFor(() => expect(billingApi.resolver).toHaveBeenCalledWith('sol', true, ''));
 });
 it('recupera caja abierta y una modalidad gratuita no ofrece cargo o pago', async () => {
  const d = contexto(); d.citas[0].cita.importe = 0;
  d.sesiones = [{ id: 'sesion', cajeroId: cuenta.id, fondoInicial: 100, estado: 'ABIERTA', fechaApertura: '2026-10-12T14:00:00Z', fechaCierre: null, efectivoDeclarado: null, diferencia: null, motivoDiferencia: null }];
  vi.mocked(billingApi.contexto).mockResolvedValue(d);
  render(<BillingView cuenta={cuenta} onExpired={vi.fn()} />);
  fireEvent.change(await screen.findByLabelText('Cita'), { target: { value: 'cita' } });
  expect(screen.getByText(/Modalidad sin cargo/)).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Generar cargo' })).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Abrir caja' })).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Confirmar cierre de caja' })).toBeInTheDocument();
 });
 it('conserva clave del pago al reintentar después de error de red', async () => {
  const d = contexto();
  d.citas[0].cargos = [{ cargo: { id: 'cargo', citaId: 'cita', pacienteId: 'paciente', importe: 40, estado: 'PENDIENTE' }, movimientos: [], totalPagado: 0, totalReversiones: 0, saldoPendiente: 40 }];
  d.sesiones = [{ id: 'sesion', cajeroId: cuenta.id, fondoInicial: 100, estado: 'ABIERTA', fechaApertura: '2026-10-12T14:00:00Z', fechaCierre: null, efectivoDeclarado: null, diferencia: null, motivoDiferencia: null }];
  vi.mocked(billingApi.contexto).mockResolvedValue(d); vi.mocked(billingApi.pagar).mockRejectedValue(new Error('Error de red'));
  render(<BillingView cuenta={cuenta} onExpired={vi.fn()} />);
  fireEvent.change(await screen.findByLabelText('Cita'), { target: { value: 'cita' } });
  fireEvent.change(screen.getByLabelText('Importe del pago'), { target: { value: '10' } });
  fireEvent.click(screen.getByRole('button', { name: 'Registrar pago' }));
  await screen.findByRole('alert');
  await waitFor(() => expect(screen.getByRole('button', { name: 'Registrar pago' })).toBeEnabled());
  fireEvent.click(screen.getByRole('button', { name: 'Registrar pago' }));
  await waitFor(() => expect(billingApi.pagar).toHaveBeenCalledTimes(2));
  expect(vi.mocked(billingApi.pagar).mock.calls[0]).toEqual(vi.mocked(billingApi.pagar).mock.calls[1]);
 });

 it('caja usa la sesión común y desaparece al revocar su permiso', async () => {
  const props = { onExit: vi.fn(), onRefresh: vi.fn() };
  const vista = render(<SessionView cuenta={cuenta} {...props} />);
  fireEvent.click(screen.getByRole('button', { name: 'Caja y cobros' }));
  await screen.findByRole('heading', { name: 'Caja y cobros' });
  expect(screen.getAllByRole('button', { name: 'Cerrar sesión' })).toHaveLength(1);
  vista.rerender(<SessionView cuenta={{ ...cuenta, roles: ['REC'], permisos: ['CITAS_CONSULTAR', 'PACIENTES_GESTIONAR'] }} {...props} />);
  expect(screen.queryByRole('heading', { name: 'Caja y cobros' })).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Caja y cobros' })).not.toBeInTheDocument();
  expect(screen.getByRole('heading', { name: 'Hola, caja' })).toBeVisible();
 });
});
