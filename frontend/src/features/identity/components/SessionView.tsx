import { RolesPanel } from './RolesPanel';
import { RegistrarPacienteForm } from '../../administration/components/RegistrarPacienteForm';
import { BuscarPacientesPanel } from '../../administration/components/BuscarPacientesPanel';
import { ServiciosPanel } from '../../administration/components/ServiciosPanel';
import { ConsultoriosPanel } from '../../administration/components/ConsultoriosPanel';
import { ProfesionalesPanel } from '../../administration/components/ProfesionalesPanel';
import { DisponibilidadPanel } from '../../administration/components/DisponibilidadPanel';
import { useCallback, useEffect, useRef, useState } from 'react';
import { auth, type Cuenta, type Rol, type Permiso } from '../api/auth';
import { ApiError } from '../../../shared/api/http';
import { useSessionActivity } from '../hooks/useSessionActivity';

const nombresRoles: Record<Rol, string> = { REC: 'Recepción / admisión', CAJ: 'Caja', ADM: 'Administrador del centro', PRO: 'Profesional de salud' };

export function SessionView({ cuenta, onExit, onRefresh }: { cuenta: Cuenta; onExit: (message: string) => void; onRefresh: (cuenta: Cuenta) => void }) {
  const [vista, setVista] = useState('inicio');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [connected, setConnected] = useState(true);
  const title = useRef<HTMLHeadingElement>(null);
  const expired = useCallback(() => onExit('Tu sesión terminó. Vuelve a ingresar para continuar.'), [onExit]);
  useSessionActivity(cuenta.id, expired, setConnected, onRefresh);
  const accesos = [
    { id: 'roles', nombre: 'Usuarios y roles', permiso: 'ROLES_GESTIONAR' },
    { id: 'registro', nombre: 'Registrar paciente', permiso: 'PACIENTES_GESTIONAR' },
    { id: 'pacientes', nombre: 'Buscar pacientes', permiso: 'PACIENTES_GESTIONAR' },
    { id: 'servicios', nombre: 'Gestionar servicios', permiso: 'SERVICIOS_GESTIONAR' },
    { id: 'consultorios', nombre: 'Gestionar consultorios', permiso: 'CONSULTORIOS_GESTIONAR' },
    { id: 'profesionales', nombre: 'Gestionar profesionales', permiso: 'PROFESIONALES_GESTIONAR' },
    { id: 'disponibilidad', nombre: 'Gestionar disponibilidad', permiso: 'DISPONIBILIDAD_GESTIONAR' },
  ].filter(acceso => cuenta.permisos.includes(acceso.permiso as Permiso));
  const accesoActual = accesos.find(acceso => acceso.id === vista);
  const vistaActual = accesoActual?.id ?? 'inicio';
  useEffect(() => { if (vistaActual === 'inicio') title.current?.focus(); }, [vistaActual]);
  useEffect(() => { if (vista !== vistaActual) setVista(vistaActual); }, [vista, vistaActual]);

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
        <button type="button" aria-current={vistaActual === 'inicio' ? 'page' : undefined} onClick={() => setVista('inicio')}>Inicio</button>
        {accesos.map(acceso => <button key={acceso.id} type="button" aria-label={acceso.id === 'roles' ? (cuenta.permisos.includes('CUENTAS_GESTIONAR') ? 'Administrar cuentas y roles' : 'Administrar roles') : undefined} aria-current={vistaActual === acceso.id ? 'page' : undefined} onClick={() => setVista(acceso.id)}>{acceso.nombre}</button>)}
      </nav>
      <div className="sidebar-profile"><span className="avatar" aria-hidden="true">{cuenta.identificador.slice(0, 2).toUpperCase()}</span><div><strong>{cuenta.identificador}</strong><small>Sesión interna</small></div></div>
      <button className="logout-button" type="button" data-no-activity disabled={busy} onClick={logout}>{busy ? 'Cerrando sesión…' : 'Cerrar sesión'}</button>
    </aside>
    <div className="workspace-body">
      <header className="workspace-header"><span>Inicio{accesoActual && ` / ${accesoActual.nombre}`}</span><span className="avatar" aria-label={`Cuenta: ${cuenta.identificador}`}>{cuenta.identificador.slice(0, 2).toUpperCase()}</span></header>
      <main id="main-content" className="workspace-content" tabIndex={-1}>
        {!connected && <p className="error" role="status">No pudimos comprobar la sesión. Revisa tu conexión; volveremos a comprobarla.</p>}
        {error && <p className="error" role="alert">{error}</p>}
        {vistaActual === 'roles' && <RolesPanel gestionarCuentas={cuenta.permisos.includes('CUENTAS_GESTIONAR')} onRefresh={onRefresh} onExpired={expired} />}
        {vistaActual === 'registro' && <RegistrarPacienteForm onExpired={expired} />}
        {vistaActual === 'pacientes' && <BuscarPacientesPanel onExpired={expired} />}
        {vistaActual === 'servicios' && <ServiciosPanel onExpired={expired} />}
        {vistaActual === 'consultorios' && <ConsultoriosPanel onExpired={expired} />}
        {vistaActual === 'profesionales' && <ProfesionalesPanel onExpired={expired} />}
        {vistaActual === 'disponibilidad' && <DisponibilidadPanel onExpired={expired} />}
        {vistaActual === 'inicio' && <section className="session-card" aria-labelledby="session-title">
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
    <p className="access-help">Usa el menú para acceder a las funciones disponibles según los permisos de tu cuenta.</p>
    <p className="access-help">Cierra tu sesión cuando termines, especialmente en equipos compartidos.</p>
        </section>}
      </main>
    </div>
  </div>;
}
