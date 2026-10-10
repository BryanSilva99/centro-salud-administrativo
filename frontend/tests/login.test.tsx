import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import axe from 'axe-core';
import { App } from '../src/app/App';
import { auth } from '../src/features/identity/api/auth';

const cuenta = { id: 'f12e50d4-1dfa-4aa1-9c1d-b37ccbf6ab55', identificador: 'prueba', activa: true };
const response = (status: number, body?: unknown) => new Response(body === undefined ? null : JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
function install(server: (url: string, options: RequestInit) => Response | Promise<Response>) {
  const fetch = vi.fn((url: string, options: RequestInit = {}) => Promise.resolve(server(url, options)));
  vi.stubGlobal('fetch', fetch);
  return fetch;
}
const signedOut = (url: string) => url.endsWith('/csrf') ? response(200, { headerName: 'X-CSRF-TOKEN', token: 'token-de-prueba' }) : response(401);
async function fill() {
  fireEvent.change(await screen.findByLabelText('Identificador'), { target: { value: 'prueba' } });
  fireEvent.change(screen.getByLabelText('Contraseña'), { target: { value: 'entrada-de-prueba' } });
}

describe('Acceso del personal', () => {
  it('presenta campos etiquetados y permite mostrar la contraseña sin enviar el formulario', async () => {
    const fetch = install(signedOut);
    render(<App />);
    await fill();
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('type', 'password');
    fireEvent.click(screen.getByRole('button', { name: 'Mostrar contraseña' }));
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('type', 'text');
    expect(fetch.mock.calls.filter(([url]) => url.endsWith('/login'))).toHaveLength(0);
  });

  it('obtiene CSRF, inicia sesión y cierra sesión usando el token renovado', async () => {
    let logged = false;
    let token = 0;
    const fetch = install((url, options) => {
      if (url.endsWith('/csrf')) return response(200, { headerName: 'X-CSRF-TOKEN', token: `token-${++token}` });
      if (url.endsWith('/me')) return response(logged ? 200 : 401, logged ? cuenta : undefined);
      if (url.endsWith('/login')) { expect(options.headers).toMatchObject({ 'X-CSRF-TOKEN': 'token-1' }); logged = true; return response(200, cuenta); }
      if (url.endsWith('/logout')) { expect(options.headers).toMatchObject({ 'X-CSRF-TOKEN': 'token-2' }); logged = false; return response(204); }
      throw new Error('Ruta inesperada');
    });
    const storage = vi.spyOn(Storage.prototype, 'setItem');
    render(<App />); await fill();
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(await screen.findByRole('heading', { name: 'Hola, prueba' })).toBeVisible();
    expect(fetch.mock.calls.find(([url]) => url.endsWith('/login'))?.[1]).toMatchObject({ credentials: 'same-origin', method: 'POST' });
    expect(storage).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }));
    expect(await screen.findByText('Cerraste sesión correctamente.')).toBeVisible();
    expect(screen.getByLabelText('Contraseña')).toHaveValue('');
  });

  it('muestra el rechazo genérico, limpia la contraseña y no reintenta el login', async () => {
    const fetch = install(signedOut);
    render(<App />); await fill();
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Identificador o contraseña incorrectos');
    expect(screen.getByLabelText('Contraseña')).toHaveValue('');
    expect(fetch.mock.calls.filter(([url]) => url.endsWith('/login'))).toHaveLength(1);
  });

  it('restaura una sesión existente al cargar la página', async () => {
    install(() => response(200, cuenta));
    render(<App />);
    expect(await screen.findByRole('heading', { name: 'Hola, prueba' })).toBeVisible();
    expect(screen.queryByLabelText('Contraseña')).not.toBeInTheDocument();
  });

  it('distingue una caída de conexión de una sesión anónima y ofrece reintento', async () => {
    let online = false;
    install(url => { if (!online) throw new TypeError('Failed to fetch'); return signedOut(url); });
    render(<App />);
    expect(await screen.findByRole('heading', { name: 'No pudimos conectar' })).toBeVisible();
    online = true;
    fireEvent.click(screen.getByRole('button', { name: 'Volver a intentar' }));
    expect(await screen.findByLabelText('Identificador')).toBeVisible();
  });

  it('conserva la pantalla y permite reintentar si el logout falla', async () => {
    install(url => url.endsWith('/me') ? response(200, cuenta) : url.endsWith('/csrf') ? signedOut(url) : response(503));
    render(<App />);
    fireEvent.click(await screen.findByRole('button', { name: 'Cerrar sesión' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('El servicio no está disponible');
    expect(screen.getByRole('heading', { name: 'Hola, prueba' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeEnabled();
  });

  it('vuelve al login si otra pestaña invalida la sesión', async () => {
    let logged = true;
    install(() => response(logged ? 200 : 401, logged ? cuenta : undefined));
    render(<App />); await screen.findByRole('heading', { name: 'Hola, prueba' });
    logged = false;
    fireEvent(document, new Event('visibilitychange'));
    expect(await screen.findByText('Tu sesión terminó. Vuelve a ingresar para continuar.')).toBeVisible();
  });

  it('no renueva actividad por eventos sintéticos ni por consultar la sesión', async () => {
    const fetch = install(() => response(200, cuenta));
    render(<App />); await screen.findByRole('heading', { name: 'Hola, prueba' });
    vi.spyOn(Date, 'now').mockReturnValue(Date.now() + 65_000);
    fireEvent.pointerDown(document); fireEvent.keyDown(document, { key: 'a' });
    fireEvent(document, new Event('visibilitychange'));
    await waitFor(() => expect(fetch.mock.calls.filter(([url]) => url.endsWith('/me')).length).toBeGreaterThan(1));
    expect(fetch.mock.calls.filter(([url]) => url.endsWith('/activity'))).toHaveLength(0);
  });

  it('no presenta infracciones Axe en el formulario y su estado de error (sin evaluar contraste en jsdom)', async () => {
    install(signedOut);
    const { container } = render(<App />); await fill();
    const options = { rules: { 'color-contrast': { enabled: false } } };
    expect((await axe.run(container, options)).violations).toEqual([]);
    fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    await screen.findByRole('alert');
    expect((await axe.run(container, options)).violations).toEqual([]);
  });
});

describe('Cliente de sesión', () => {
  it('no repite solicitudes de escritura ante un rechazo CSRF', async () => {
    const fetch = install(url => url.endsWith('/csrf') ? signedOut(url) : response(403));
    await expect(auth.login('prueba', 'entrada-de-prueba')).rejects.toMatchObject({ status: 403 });
    expect(fetch).toHaveBeenCalledTimes(2);
  });
});
