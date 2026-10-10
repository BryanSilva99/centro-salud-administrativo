import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import axe from 'axe-core';
import { RolesPanel } from '../src/features/identity/components/RolesPanel';
import { SessionView } from '../src/features/identity/components/SessionView';
import type { Cuenta } from '../src/features/identity/api/auth';
const cuenta = { id: 'cuenta-prueba', identificador: 'recepcion', activa: true, roles: ['REC'], version: 4 };
const admin = { ...cuenta, roles: ['ADM'], permisos: ['CUENTAS_CONSULTAR', 'ROLES_GESTIONAR'] };
const response = (status: number, body: unknown) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
function install(save: (options: RequestInit) => Response) {
  vi.stubGlobal('fetch', vi.fn(async (url: string, options: RequestInit = {}) => {
    if (url.endsWith('/csrf')) return response(200, { headerName: 'X-CSRF-TOKEN', token: 'csrf-prueba' });
    if (url.endsWith('/me')) return response(200, admin);
    if (options.method === 'PUT') return save(options);
    if (url.includes('/auditoria')) return response(200, { elementos: [{ id: 'cambio', actorId: 'administrador-prueba', anteriores: [], nuevos: ['REC'], fecha: '2026-10-10T10:00:00Z' }], hayMas: false });
    if (url.includes('?busqueda=')) return response(200, { elementos: [cuenta], hayMas: false });
    return response(200, cuenta);
  }));
}
async function seleccionar() {
  fireEvent.click(await screen.findByRole('button', { name: 'recepcion · Activa' }));
  await screen.findByRole('checkbox', { name: 'Caja' });
}
describe('Administración de roles', () => {
  it('envía la versión y CSRF, muestra auditoría y actualiza la sesión', async () => {
    const save = vi.fn((options: RequestInit) => {
      expect(options.headers).toMatchObject({ 'X-CSRF-TOKEN': 'csrf-prueba' });
      expect(JSON.parse(options.body as string)).toEqual({ roles: ['REC', 'CAJ'], version: 4 });
      return response(200, { ...cuenta, roles: ['REC', 'CAJ'], version: 7 });
    });
    install(save); const refresh=vi.fn();
    render(<RolesPanel onRefresh={refresh} onExpired={vi.fn()} />); await seleccionar();
    expect(await screen.findByText('Responsable (ID): administrador-prueba')).toBeVisible();
    fireEvent.click(screen.getByRole('checkbox', { name: 'Caja' }));
    fireEvent.click(screen.getByRole('button', { name: 'Guardar roles' }));
    await waitFor(() => expect(refresh).toHaveBeenCalledWith(admin));expect(save).toHaveBeenCalledTimes(1);
  });
  it('bloquea una edición obsoleta hasta recargar', async () => {
    install(() => response(409, { code: 'CONFLICTO' }));
    render(<RolesPanel onRefresh={vi.fn()} onExpired={vi.fn()} />);await seleccionar();
    fireEvent.click(screen.getByRole('checkbox', { name: 'Caja' }));fireEvent.click(screen.getByRole('button', { name: 'Guardar roles' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Otra persona cambió');
    expect(screen.getByRole('button', { name: 'Guardar roles' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: 'Recargar cuenta' }));
    await waitFor(() => expect(screen.getByRole('checkbox', { name: 'Caja' })).not.toBeChecked());
  });
  it('explica el rechazo al retirar el último administrador', async () => {
    install(() => response(409, { code: 'ULTIMO_ADMIN' }));
    render(<RolesPanel onRefresh={vi.fn()} onExpired={vi.fn()} />); await seleccionar();
    fireEvent.click(screen.getByRole('checkbox', { name: 'Caja' }));fireEvent.click(screen.getByRole('button', { name: 'Guardar roles' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('al menos un administrador activo');
  });
  it('oculta la administración al perder el permiso en la sesión', async () => {
    install(() => response(200, cuenta));
    const props = { onRefresh: vi.fn(), onExit: vi.fn() };
    const { rerender } = render(<SessionView cuenta={admin as Cuenta} {...props} />);
    fireEvent.click(await screen.findByRole('button', { name: 'Administrar roles' }));
    expect(await screen.findByRole('heading', { name: 'Administrar roles' })).toBeVisible();
    rerender(<SessionView cuenta={{ ...admin, roles: ['REC'], permisos: [] } as Cuenta} {...props} />);
    expect(screen.queryByRole('heading', { name: 'Administrar roles' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Administrar roles' })).not.toBeInTheDocument();
  });
  it('mantiene etiquetas y estructura accesibles', async () => {
    install(() => response(200, cuenta));
    const { container }=render(<main><RolesPanel onRefresh={vi.fn()} onExpired={vi.fn()} /></main>);await seleccionar();
    const result=await axe.run(container, { rules: { 'color-contrast': { enabled: false } } });
    expect(result.violations).toEqual([]);
  });
});
