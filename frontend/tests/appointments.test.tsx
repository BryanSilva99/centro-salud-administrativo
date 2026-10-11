import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import axe from 'axe-core';
import { SessionView } from '../src/features/identity/components/SessionView';
import { AppointmentsWorkspace } from '../src/features/appointments/components/AppointmentsWorkspace';

const cuenta: import('../src/features/identity/api/auth').Cuenta = { id: 'f12e50d4-1dfa-4aa1-9c1d-b37ccbf6ab55', identificador: 'prueba', activa: true, roles: ['REC'], permisos: ['CITAS_CONSULTAR'] };
const context = {
  pacientes: [{ id: 'p1', nombre: 'Paciente Demo 01', activo: true }],
  profesionales: [{ id: 'pro1', nombre: 'Profesional Demo 01', especialidad: 'Medicina general', activo: true }],
  servicios: [{ id: 's1', nombre: 'Consulta médica general', activo: true, duracionMinutos: 30, modalidades: [{ codigo: 'PARTICULAR', nombre: 'Particular', importe: 50 }] }],
  consultorios: [{ id: 'r1', nombre: 'Consultorio Demo 01', activo: true }],
  capacidades: ['CONSULTAR', 'PROGRAMAR', 'REPROGRAMAR', 'REGISTRAR_LLEGADA', 'CONSULTAR_ATENCION', 'INICIAR_ATENCION', 'FINALIZAR_ATENCION', 'CONSULTAR_DASHBOARD', 'CANCELAR', 'REGISTRAR_AUSENCIA', 'REGISTRAR_RETIRO'],
  profesionalAsignadoId: 'pro1',
};
const cita = {
  id: 'c1', pacienteId: 'p1', paciente: 'Paciente Demo 01', profesionalId: 'pro1', profesional: 'Profesional Demo 01', especialidad: 'Medicina general',
  servicioId: 's1', servicio: 'Consulta médica general', consultorioId: 'r1', consultorio: 'Consultorio Demo 01', inicio: '2026-10-12T09:00:00-05:00', fin: '2026-10-12T09:30:00-05:00',
  estado: 'PROGRAMADA', duracionMinutos: 30, modalidad: 'PARTICULAR', modalidadNombre: 'Particular', importe: 50, llegadaEn: null, atencionIniciadaEn: null, atencionFinalizadaEn: null, version: 0,
};
const response = (status: number, body: unknown) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });

function install(initialAgenda: unknown[] = [], attentionBody?: unknown, dashboardBody?: unknown, capabilities = context.capacidades) {
  const fetch = vi.fn((url: string, options: RequestInit = {}) => {
    if (url.endsWith('/api/auth/me')) return Promise.resolve(response(200, cuenta));
    if (url.endsWith('/api/citas/contexto')) return Promise.resolve(response(200, {...context, capacidades: capabilities}));
    if ((url.includes('/atencion/inicio') || url.includes('/atencion/finalizacion')) && options.method === 'PUT') return Promise.resolve(response(200, { ...cita, estado: 'EN_ATENCION', llegadaEn: '2026-10-10T14:05:00-05:00', atencionIniciadaEn: '2026-10-10T14:10:00-05:00', version: 2 }));
    if (url.includes('/cancelacion') && options.method === 'PUT') return Promise.resolve(response(200, { cita: { ...cita, estado: 'CANCELADA', tipoCierre: 'CANCELACION', motivoCierre: 'Solicitud del paciente', cerradaEn: '2026-10-10T14:20:00-05:00', version: 1 }, gestionEconomicaPendiente: true }));
    if (url.includes('/espera/retiro') && options.method === 'PUT') return Promise.resolve(response(200, { cita: { ...cita, estado: 'CANCELADA', tipoCierre: 'RETIRO_ESPERA', motivoCierre: 'Paciente se retira', cerradaEn: '2026-10-10T14:20:00-05:00', version: 2 }, gestionEconomicaPendiente: false }));
    if (url.includes('/ausencia') && options.method === 'PUT') return Promise.resolve(response(200, { ...cita, estado: 'AUSENTE', tipoCierre: 'AUSENCIA', cerradaEn: '2026-10-10T14:20:00-05:00', version: 1 }));
    if (url.includes('/api/citas/atencion')) return Promise.resolve(response(200, attentionBody || { fecha: '2026-10-10', consultadaEn: '2026-10-10T14:15:00-05:00', enEspera: [], enAtencion: [] }));
    if (url.includes('/api/citas/dashboard')) return Promise.resolve(response(200, dashboardBody || { fecha: '2026-10-10', profesionalId: null, servicioId: null, consultorioId: null, actualizadoEn: '2026-10-10T14:15:00-05:00', porEstado: { PROGRAMADA: 0, EN_ESPERA: 0, EN_ATENCION: 0, ATENDIDA: 0, CANCELADA: 0, AUSENTE: 0 }, total: 0, enEspera: 0, atendidas: 0, ausentes: 0 }));
    if (url.includes('/api/citas/agenda')) return Promise.resolve(response(200, initialAgenda));
    if (url.includes('/api/citas/disponibilidad')) return Promise.resolve(response(200, [{ inicio: '2026-10-12T09:00:00-05:00', fin: '2026-10-12T09:30:00-05:00' }]));
    if (url.endsWith('/api/auth/csrf')) return Promise.resolve(response(200, { headerName: 'X-CSRF-TOKEN', token: 'token' }));
    if (url.endsWith('/api/citas')) return Promise.resolve(response(201, cita));
    if (url.includes('/reprogramaciones')) return Promise.resolve(response(200, { ...cita, inicio: '2026-10-12T10:00:00-05:00', fin: '2026-10-12T10:30:00-05:00', version: 1 }));
    if (url.endsWith('/api/auth/activity')) return Promise.resolve(new Response(null, { status: 204 }));
    if (url.endsWith('/api/auth/logout')) return Promise.resolve(new Response(null, { status: 204 }));
    throw new Error(`Ruta inesperada: ${url} ${options.method || 'GET'}`);
  });
  vi.stubGlobal('fetch', fetch);
  return fetch;
}

describe('Agenda de citas', () => {
  it('carga catálogo, consulta horarios y programa una cita', async () => {
    const fetch = install();
    render(<AppointmentsWorkspace cuenta={cuenta} onExit={vi.fn()} />);
    expect(await screen.findByRole('heading', { name: 'Agenda del día' })).toBeVisible();
    const dates = screen.getAllByLabelText('Fecha');
    fireEvent.change(dates[1], { target: { value: '2026-10-12' } });
    expect(await screen.findByRole('radio', { name: '09:00' })).toBeVisible();
    fireEvent.click(screen.getByRole('radio', { name: '09:00' }));
    fireEvent.click(screen.getByRole('button', { name: /Programar cita/ }));
    await waitFor(() => expect(fetch.mock.calls.some(([url, options]) => url.endsWith('/api/citas') && options?.method === 'POST')).toBe(true));
    expect(await screen.findByText('La cita se programó correctamente.')).toBeVisible();
  });

  it('abre el diálogo y envía una reprogramación con motivo y versión', async () => {
    const fetch = install([cita]);
    render(<AppointmentsWorkspace cuenta={cuenta} onExit={vi.fn()} />);
    fireEvent.click(await screen.findByRole('button', { name: 'Reprogramar' }));
    fireEvent.change(screen.getByLabelText('Nueva fecha'), { target: { value: '2026-10-12' } });
    fireEvent.change(screen.getByLabelText('Nueva hora'), { target: { value: '10:00' } });
    fireEvent.change(screen.getByLabelText('Motivo'), { target: { value: 'Solicitud del paciente' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar cambio' }));
    await waitFor(() => expect(fetch.mock.calls.some(([url, options]) => url.includes('/reprogramaciones') && options?.method === 'POST')).toBe(true));
    const call = fetch.mock.calls.find(([url]) => url.includes('/reprogramaciones'));
    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({ fecha: '2026-10-12', hora: '10:00', motivo: 'Solicitud del paciente', version: 0 });
  });

  it('consulta la sala de espera e inicia una atención con la versión leída', async () => {
    const waiting = { ...cita, estado: 'EN_ESPERA', llegadaEn: '2026-10-10T14:05:00-05:00', version: 1 };
    const fetch = install([], { fecha: '2026-10-10', consultadaEn: '2026-10-10T14:15:00-05:00', enEspera: [waiting], enAtencion: [] });
    vi.stubGlobal('confirm', vi.fn(() => true));
    render(<AppointmentsWorkspace cuenta={cuenta} onExit={vi.fn()} />);
    fireEvent.click(await screen.findByRole('tab', { name: 'Atención' }));
    expect(await screen.findByText('Paciente Demo 01')).toBeVisible();
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar atención' }));
    await waitFor(() => expect(fetch.mock.calls.some(([url, options]) => url.includes('/atencion/inicio') && options?.method === 'PUT')).toBe(true));
    const call = fetch.mock.calls.find(([url]) => url.includes('/atencion/inicio'));
    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({ version: 1 });
    expect(await screen.findByText('La atención inició correctamente.')).toBeVisible();
  });

  it('muestra y actualiza el dashboard operativo', async () => {
    install([], undefined, { fecha: '2026-10-10', profesionalId: null, servicioId: null, consultorioId: null, actualizadoEn: '2026-10-10T14:15:00-05:00', porEstado: { PROGRAMADA: 2, EN_ESPERA: 1, EN_ATENCION: 0, ATENDIDA: 3, CANCELADA: 0, AUSENTE: 0 }, total: 6, enEspera: 1, atendidas: 3, ausentes: 0 });
    render(<AppointmentsWorkspace cuenta={cuenta} onExit={vi.fn()} />);
    fireEvent.click(await screen.findByRole('tab', { name: 'Dashboard' }));
    expect(await screen.findByRole('heading', { name: 'Dashboard operativo' })).toBeVisible();
    expect(await screen.findByText('Total de citas')).toBeVisible();
    expect(screen.getByText('6')).toBeVisible();
  });

  it('cancela una cita con motivo y muestra la advertencia economica', async () => {
    const fetch = install([cita]);
    render(<AppointmentsWorkspace cuenta={cuenta} onExit={vi.fn()} />);
    fireEvent.click(await screen.findByRole('button', { name: 'Cancelar cita' }));
    fireEvent.change(screen.getByLabelText('Motivo'), { target: { value: 'Solicitud del paciente' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar cierre' }));
    await waitFor(() => expect(fetch.mock.calls.some(([url, options]) => url.includes('/cancelacion') && options?.method === 'PUT')).toBe(true));
    const call = fetch.mock.calls.find(([url]) => url.includes('/cancelacion'));
    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({ motivo: 'Solicitud del paciente', version: 0 });
    expect(await screen.findByText(/gesti/)).toBeVisible();
  });

  it('permite retirar una cita en espera conservando el motivo', async () => {
    const waiting = { ...cita, estado: 'EN_ESPERA', llegadaEn: '2026-10-10T14:05:00-05:00', version: 1 };
    const fetch = install([], { fecha: '2026-10-10', consultadaEn: '2026-10-10T14:15:00-05:00', enEspera: [waiting], enAtencion: [] });
    render(<AppointmentsWorkspace cuenta={cuenta} onExit={vi.fn()} />);
    fireEvent.click(await screen.findByRole('tab', { name: /Atención/ }));
    fireEvent.click(await screen.findByRole('button', { name: 'Registrar retiro' }));
    fireEvent.change(screen.getByLabelText('Motivo'), { target: { value: 'Paciente se retira' } });
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar cierre' }));
    await waitFor(() => expect(fetch.mock.calls.some(([url, options]) => url.includes('/espera/retiro') && options?.method === 'PUT')).toBe(true));
    const call = fetch.mock.calls.find(([url]) => url.includes('/espera/retiro'));
    expect(JSON.parse(String(call?.[1]?.body))).toMatchObject({ motivo: 'Paciente se retira', version: 1 });
  });

  it('ofrece ausencia cuando el intervalo ya termino', async () => {
    const pasada = { ...cita, inicio: '2026-10-09T09:00:00-05:00', fin: '2026-10-09T09:30:00-05:00' };
    const fetch = install([pasada]);
    render(<AppointmentsWorkspace cuenta={cuenta} onExit={vi.fn()} />);
    fireEvent.click(await screen.findByRole('button', { name: 'Registrar ausencia' }));
    fireEvent.click(screen.getByRole('button', { name: 'Confirmar ausencia' }));
    await waitFor(() => expect(fetch.mock.calls.some(([url, options]) => url.includes('/ausencia') && options?.method === 'PUT')).toBe(true));
  });

  it('mantiene el workspace sin infracciones Axe básicas', async () => {
    install();
    const { container } = render(<AppointmentsWorkspace cuenta={cuenta} onExit={vi.fn()} />);
    await screen.findByRole('heading', { name: 'Agenda del día' });
    expect((await axe.run(container, { rules: { 'color-contrast': { enabled: false } } })).violations).toEqual([]);
  });
  it('permite consultar a administración sin ofrecer reserva ni pedir disponibilidad', async () => {
    const fetch=install([], undefined, undefined, ['CONSULTAR','CONSULTAR_GLOBAL','CONSULTAR_ATENCION','CONSULTAR_DASHBOARD']);
    render(<AppointmentsWorkspace cuenta={{...cuenta, roles:['ADM']}} onExit={vi.fn()} />);
    await screen.findByText('No hay citas para los filtros seleccionados.');
    expect(screen.queryByRole('heading',{name:'Nueva cita'})).not.toBeInTheDocument();
    expect(fetch.mock.calls.some(([url])=>url.includes('/disponibilidad'))).toBe(false);
  });

  it('abre citas desde el menú integrado y las retira al revocar el permiso', async () => {
    install();
    const props={onExit:vi.fn(),onRefresh:vi.fn()};
    const {rerender}=render(<SessionView cuenta={cuenta} {...props} />);
    fireEvent.click(screen.getByRole('button',{name:'Agenda y citas'}));
    expect(await screen.findByRole('heading',{name:'Nueva cita'})).toBeVisible();
    expect(screen.getAllByRole('button',{name:'Cerrar sesión'})).toHaveLength(1);
    rerender(<SessionView cuenta={{...cuenta,roles:[],permisos:[]}} {...props} />);
    expect(screen.queryByRole('heading',{name:'Nueva cita'})).not.toBeInTheDocument();
    expect(screen.queryByRole('button',{name:'Agenda y citas'})).not.toBeInTheDocument();
  });

});
