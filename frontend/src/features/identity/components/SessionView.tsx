import { RolesPanel } from './RolesPanel';
import { useCallback, useEffect, useRef, useState } from 'react';
import { auth, type Cuenta, type Rol } from '../api/auth';
import { ApiError } from '../../../shared/api/http';
import { useSessionActivity } from '../hooks/useSessionActivity';

const nombresRoles: Record<Rol, string> = { REC: 'Recepción / admisión', CAJ: 'Caja', ADM: 'Administrador del centro', PRO: 'Profesional de salud' };

export function SessionView({ cuenta, onExit, onRefresh }: { cuenta: Cuenta; onExit: (message: string) => void; onRefresh: (cuenta: Cuenta) => void }) {
  const [showRoles, setShowRoles] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [connected, setConnected] = useState(true);
  const title = useRef<HTMLHeadingElement>(null);
  const expired = useCallback(() => onExit('Tu sesión terminó. Vuelve a ingresar para continuar.'), [onExit]);
  useSessionActivity(cuenta.id, expired, setConnected, onRefresh);
  const puedeAdministrar = cuenta.permisos.includes('ROLES_GESTIONAR');
  const administracionAbierta = showRoles && puedeAdministrar;
  useEffect(() => { if (!administracionAbierta) title.current?.focus(); }, [administracionAbierta]);
  useEffect(() => { if (!puedeAdministrar) setShowRoles(false); }, [puedeAdministrar]);

  async function logout() {
    setBusy(true); setError('');
    try { await auth.logout(); onExit('Cerraste sesión correctamente.'); }
    catch (failure) {
      if (failure instanceof ApiError && failure.status === 401) expired();
      else setError(failure instanceof Error ? failure.message : 'No pudimos cerrar sesión. Vuelve a intentarlo.');
    } finally { setBusy(false); }
  }
  return <div className="workspace">
    <a className="skip-link" href="#main-content">Saltar al contenido</a>
    <aside className="sidebar" aria-label="Área del personal">
      <a className="brand" href="/" aria-label="Centro de Salud, inicio"><span className="brand-mark" aria-hidden="true">+</span><span>Centro de Salud<small>Sistema administrativo</small></span></a>
      <p className="nav-caption">Operación</p>
      <nav aria-label="Navegación principal">
        <button type="button" aria-current={!administracionAbierta ? 'page' : undefined} onClick={() => setShowRoles(false)}>Inicio</button>
        {puedeAdministrar && <button type="button" aria-label={cuenta.permisos.includes('CUENTAS_GESTIONAR') ? 'Administrar cuentas y roles' : 'Administrar roles'} aria-current={administracionAbierta ? 'page' : undefined} onClick={() => setShowRoles(true)}>Usuarios y roles</button>}
      </nav>
      <div className="sidebar-profile"><span className="avatar" aria-hidden="true">{cuenta.identificador.slice(0, 2).toUpperCase()}</span><div><strong>{cuenta.identificador}</strong><small>Sesión interna</small></div></div>
      <button className="logout-button" type="button" data-no-activity disabled={busy} onClick={logout}>{busy ? 'Cerrando sesión…' : 'Cerrar sesión'}</button>
    </aside>
    <div className="workspace-body">
      <header className="workspace-header"><span>Inicio{administracionAbierta && ' / Usuarios y roles'}</span><span className="avatar" aria-label={`Cuenta: ${cuenta.identificador}`}>{cuenta.identificador.slice(0, 2).toUpperCase()}</span></header>
      <main id="main-content" className="workspace-content" tabIndex={-1}>
        {!connected && <p className="error" role="status">No pudimos comprobar la sesión. Revisa tu conexión; volveremos a comprobarla.</p>}
        {error && <p className="error" role="alert">{error}</p>}
        {administracionAbierta ? <RolesPanel gestionarCuentas={cuenta.permisos.includes('CUENTAS_GESTIONAR')} onRefresh={onRefresh} onExpired={expired} /> : <section className="session-card" aria-labelledby="session-title">
    <span className="status-badge"><span aria-hidden="true">●</span> Sesión iniciada</span>
    <h1 id="session-title" tabIndex={-1} ref={title}>Hola, {cuenta.identificador}</h1>
    <p className="intro">Ya accediste al sistema del centro de salud.</p>
    <dl className="account-details"><div><dt>Cuenta</dt><dd>{cuenta.identificador}</dd></div><div><dt>Estado</dt><dd>Activa</dd></div></dl>
    <div className="session-access" aria-live="polite">
      <h2>Roles asignados</h2>
      {(cuenta.roles ?? []).length > 0
        ? <ul aria-label="Roles asignados" className="role-list">{cuenta.roles.map(rol => <li key={rol}>{nombresRoles[rol]}</li>)}</ul>
        : <p className="notice">Tu cuenta todavía no tiene roles asignados. Comunícate con la administración.</p>}
      {(cuenta.permisos ?? []).includes('CUENTAS_CONSULTAR') && <p className="permission-note">Tu cuenta tiene permiso para consultar cuentas de acceso.</p>}
    </div>
    <p className="access-help">Las funciones de cada área se habilitarán conforme se integren sus módulos. Tu acceso se limita a los permisos de tu cuenta.</p>
    <p className="access-help">Cierra tu sesión cuando termines, especialmente en equipos compartidos.</p>
        </section>}
      </main>
    </div>
  </div>;
}
