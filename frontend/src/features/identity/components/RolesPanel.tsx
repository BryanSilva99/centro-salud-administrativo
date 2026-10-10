import { VinculoProfesional } from './VinculoProfesional';
import { CrearCuentaForm, EditarCuentaForm, errorCuenta } from './FormulariosCuenta';
import { cuentasApi, type CambioCuenta } from '../api/cuentas';
import { useEffect, useState } from 'react';
import { auth, type Cuenta, type Rol } from '../api/auth';
import { rolesApi, type CuentaRoles, type Cambio, type Pagina } from '../api/roles';
import { ApiError } from '../../../shared/api/http';
const nombres: Record<Rol, string> = { REC: 'Recepción / admisión', CAJ: 'Caja', ADM: 'Administrador del centro', PRO: 'Profesional de salud' };
const vacia = { elementos: [], hayMas: false };
export function RolesPanel({ onRefresh, onExpired, gestionarCuentas = false }: { gestionarCuentas?: boolean; onRefresh: (cuenta: Cuenta) => void; onExpired: () => void }) {
  const [creando, setCreando] = useState(false);
  const [historialCuenta, setHistorialCuenta] = useState<Pagina<CambioCuenta>>(vacia);
  const [paginaCuenta, setPaginaCuenta] = useState(0);
  const [busqueda, setBusqueda] = useState('');
  const [consulta, setConsulta] = useState('');
  const [pagina, setPagina] = useState(0);
  const [lista, setLista] = useState<Pagina<CuentaRoles>>(vacia);
  const [id, setId] = useState('');
  const [cuenta, setCuenta] = useState<CuentaRoles | null>(null);
  const [roles, setRoles] = useState<Rol[]>([]);
  const [historial, setHistorial] = useState<Pagina<Cambio>>(vacia);
  const [paginaAudit, setPaginaAudit] = useState(0);
  const [revision, setRevision] = useState(0);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [conflicto, setConflicto] = useState(false);
  function failure(e: unknown) {
    if (e instanceof ApiError && e.status === 401) { onExpired(); return; }
    setError(errorCuenta(e));
  }
  useEffect(() => {
    const controller = new AbortController();
    setLista(vacia);
    rolesApi.listar(consulta, pagina, controller.signal).then(lista => { if (!controller.signal.aborted) setLista(lista); }).catch(e => { if (!controller.signal.aborted) failure(e); });
    return () => controller.abort();
  }, [consulta, pagina, revision]);
  useEffect(() => {
    if (!id) return;
    const controller = new AbortController();
    setCuenta(null); setError(''); setConflicto(false); setLoading(true);
    rolesApi.cargar(id, controller.signal).then(c => { if (controller.signal.aborted) return; setCuenta(c); setRoles(c.roles); }).catch(e => { if (!controller.signal.aborted) failure(e); }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [id, revision]);
  useEffect(() => {
    if (!id) return;
    const controller = new AbortController(); setHistorial(vacia);
    rolesApi.historial(id, paginaAudit, controller.signal).then(h => { if (!controller.signal.aborted) setHistorial(h); }).catch(e => { if (!controller.signal.aborted) failure(e); });
    return () => controller.abort();
  }, [id, paginaAudit, revision]);
  useEffect(() => {
    if (!id || !gestionarCuentas) return;
    const controller = new AbortController(); setHistorialCuenta(vacia);
    cuentasApi.historial(id, paginaCuenta, controller.signal).then(h => { if (!controller.signal.aborted) setHistorialCuenta(h); }).catch(e => { if (!controller.signal.aborted) failure(e); });
    return () => controller.abort();
  }, [id, paginaCuenta, revision, gestionarCuentas]);
  async function cuentaGuardada(actualizada: CuentaRoles) {
    const nueva = creando;
    setCreando(false); setCuenta(actualizada); setId(actualizada.id); setPaginaCuenta(0); setPaginaAudit(0);
    if (nueva) { setConsulta(''); setBusqueda(''); setPagina(0); }
    setRevision(v => v + 1);
    setNotice(nueva ? 'Cuenta creada correctamente.' : 'Cuenta actualizada correctamente.');
    try { const sesion = await auth.me(); if (sesion) onRefresh(sesion); else onExpired(); }
    catch { setNotice('Cuenta guardada. No pudimos actualizar tu sesión; vuelve a comprobarla.'); }
  }
  async function guardar() {
    if (!cuenta) return;
    setBusy(true); setError(''); setNotice('');
    try {
      const actualizada = await rolesApi.guardar(cuenta, roles);
      setCuenta(actualizada); setNotice('Roles guardados. Los permisos se aplican en la siguiente solicitud.');
      setPaginaAudit(0); setRevision(v => v + 1);
      try { const sesion = await auth.me(); if (sesion) onRefresh(sesion); else onExpired(); }
      catch { setNotice('Roles guardados. No pudimos actualizar tu sesión; vuelve a comprobarla.'); }
    } catch (e) {
      setConflicto(e instanceof ApiError && e.code === 'CONFLICTO'); failure(e);
    } finally { setBusy(false); }
  }
  return <section className="roles-panel" aria-labelledby="roles-title">
    <h2 id="roles-title">{gestionarCuentas ? "Administrar cuentas y roles" : "Administrar roles"}</h2>
    <p>Selecciona una cuenta y asigna las funciones que le corresponden. Los roles se pueden combinar.</p>
    {gestionarCuentas && <button disabled={busy} onClick={() => { setCreando(v => !v); setId(''); setCuenta(null); setError(''); setNotice(''); }}>{creando ? 'Volver al directorio' : 'Nueva cuenta'}</button>}
    {creando && <CrearCuentaForm busy={busy} onBusy={setBusy} onSaved={cuentaGuardada} onExpired={onExpired} />}
    {!creando && <>
    <form onSubmit={e => { e.preventDefault(); setConsulta(busqueda); setPagina(0); setRevision(v => v + 1); }}>
      <label htmlFor="buscar-cuenta">Buscar por identificador</label>
      <input id="buscar-cuenta" value={busqueda} maxLength={254} disabled={busy} onChange={e => setBusqueda(e.target.value)} />
      <button disabled={busy}>Buscar</button>
    </form>
    <ul className="account-picker">{lista.elementos.map(c => <li key={c.id}><button type="button" disabled={busy} aria-pressed={id === c.id} onClick={() => { setId(c.id); setPaginaAudit(0); setPaginaCuenta(0); setNotice(''); }}>{c.identificador} · {c.activa ? 'Activa' : 'Inactiva'}</button></li>)}</ul>
    <div className="pagination"><button disabled={busy || pagina === 0} onClick={() => setPagina(p => p - 1)}>Cuentas anteriores</button><span>Página {pagina + 1}</span><button disabled={busy || !lista.hayMas} onClick={() => setPagina(p => p + 1)}>Más cuentas</button></div>
    {loading && <p role="status">Cargando cuenta…</p>}
    {error && <p className="error" role="alert">{error}</p>}
    {notice && <p className="notice" role="status">{notice}</p>}
    {id && <button disabled={busy} onClick={() => setRevision(v => v + 1)}>Recargar cuenta</button>}
    {cuenta && gestionarCuentas && <EditarCuentaForm key={`${cuenta.id}-${cuenta.version}-${revision}`} cuenta={cuenta} busy={busy} onBusy={setBusy} onSaved={cuentaGuardada} onExpired={onExpired} />}
    {cuenta && <form onSubmit={e => { e.preventDefault(); void guardar(); }}>
      <h3>{cuenta.identificador}</h3>
      <fieldset disabled={busy || conflicto}><legend>Roles de la cuenta</legend>
        {(Object.keys(nombres) as Rol[]).map(rol => <label className="role-option" key={rol}><input type="checkbox" checked={roles.includes(rol)} onChange={e => setRoles(actuales => e.target.checked ? [...actuales, rol] : actuales.filter(r => r !== rol))} />{nombres[rol]}</label>)}
      </fieldset>
      {roles.length === 0 && <p className="notice">Sin roles, esta cuenta podrá iniciar sesión pero no acceder a funciones de negocio.</p>}
      <button className="primary-button" disabled={busy || conflicto || [...roles].sort().join() === [...cuenta.roles].sort().join()}>{busy ? 'Guardando…' : 'Guardar roles'}</button>
    </form>}
    {cuenta && gestionarCuentas && (cuenta.roles.includes('PRO') || cuenta.profesionalId) && <VinculoProfesional key={`vinculo-${cuenta.id}-${cuenta.version}-${revision}`} cuenta={cuenta} busy={busy} onBusy={setBusy} onSaved={cuentaGuardada} onExpired={onExpired} />}
    {cuenta && <section aria-label="Historial de roles"><h3>Historial de cambios</h3>
      {historial.elementos.length === 0 && <p>No hay cambios en esta página.</p>}
      <ul className="audit-list">{historial.elementos.map(c => <li key={c.id}><time>{new Date(c.fecha).toLocaleString('es-PE')}</time><p>{c.anteriores.join(', ') || 'Sin roles'} → {c.nuevos.join(', ') || 'Sin roles'}</p><small>Responsable (ID): {c.actorId}</small></li>)}</ul>
      <div className="pagination"><button disabled={busy || paginaAudit === 0} onClick={() => setPaginaAudit(p => p - 1)}>Cambios anteriores</button><button disabled={busy || !historial.hayMas} onClick={() => setPaginaAudit(p => p + 1)}>Más cambios</button></div>
    </section>}
    {cuenta && gestionarCuentas && <section aria-label="Historial de la cuenta"><h3>Historial de la cuenta</h3>
      {historialCuenta.elementos.length === 0 && <p>No hay cambios de cuenta en esta página.</p>}
      <ul className="audit-list">{historialCuenta.elementos.map(c => <li key={c.id}><time>{new Date(c.fecha).toLocaleString('es-PE')}</time><p>{c.tipo === 'CREACION' ? 'Creación' : c.tipo === 'VINCULACION' ? 'Vínculo profesional' : 'Actualización'}: {c.identificadorAnterior && `${c.identificadorAnterior} → `}{c.identificadorNuevo}</p><p>{c.activaAnterior !== null && `${c.activaAnterior ? 'Activa' : 'Inactiva'} → `}{c.activaNueva ? 'Activa' : 'Inactiva'} · Roles: {c.roles.join(', ') || 'Sin roles'}</p><p>{c.tipo === "VINCULACION" && `Ficha: ${c.profesionalAnterior ?? "Sin vínculo"} → ${c.profesionalNuevo ?? "Sin vínculo"}`}</p><small>Responsable (ID): {c.actorId}</small></li>)}</ul>
      <div className="pagination"><button disabled={busy || paginaCuenta === 0} onClick={() => setPaginaCuenta(p => p - 1)}>Historial anterior</button><button disabled={busy || !historialCuenta.hayMas} onClick={() => setPaginaCuenta(p => p + 1)}>Más historial</button></div>
    </section>}
    </>}
  </section>;
}
