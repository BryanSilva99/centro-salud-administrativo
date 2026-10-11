import { useRef, useState } from 'react';
import type { Rol } from '../api/auth';
import type { CuentaRoles } from '../api/roles';
import { cuentasApi } from '../api/cuentas';
import { ApiError } from '../../../shared/api/http';
const nombres: Record<Rol, string> = { REC: 'Recepción / admisión', CAJ: 'Caja', ADM: 'Administrador del centro', PRO: 'Profesional de salud' };
export function errorCuenta(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'VINCULO_OCUPADO') return 'Esa ficha ya está vinculada a otra cuenta.';
    if (error.code === 'PROFESIONAL_INVALIDO') return 'Selecciona una ficha existente y activa.';
    if (error.code === 'REQUIERE_PRO') return 'La ficha necesita el rol Profesional de salud. Desvincúlala antes de retirar ese rol.';
    if (error.code === 'DUPLICADO') return 'Ese identificador ya está en uso. Elige otro.';
    if (error.code === 'ULTIMO_ADMIN') return 'Debes conservar al menos un administrador activo.';
    if (error.code === 'CONFLICTO') return 'Otra persona cambió esta cuenta. Recarga antes de editar nuevamente.';
    if (error.status === 403) return 'Ya no tienes permiso para realizar esta operación.';
  }
  return error instanceof Error ? error.message : 'No se pudo completar la operación.';
}
type Props = { busy: boolean; onBusy: (value: boolean) => void; onSaved: (cuenta: CuentaRoles) => Promise<void>; onExpired: () => void };
export function CrearCuentaForm({ busy, onBusy, onSaved, onExpired }: Props) {
  const [identificador, setIdentificador] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [confirmacion, setConfirmacion] = useState('');
  const [roles, setRoles] = useState<Rol[]>([]);
  const [error, setError] = useState('');
  const enviando = useRef(false);
  async function crear() {
    if (enviando.current) return;
    if (contrasena !== confirmacion) { setError('Las contraseñas no coinciden.'); return; }
    if ([...contrasena].length < 12 || [...contrasena].length > 128 || !contrasena.trim()) {
      setError('La contraseña debe tener entre 12 y 128 caracteres.'); return;
    }
    enviando.current = true; onBusy(true); setError('');
    try { const cuenta = await cuentasApi.crear(identificador.trim(), contrasena, roles); await onSaved(cuenta); }
    catch (e) {
      if (e instanceof ApiError && e.status === 401) onExpired();
      else setError(errorCuenta(e) + (e instanceof ApiError && e.status === 0 ? ' Antes de repetir el alta, busca el identificador para comprobar si se creó.' : ''));
    } finally { setContrasena(''); setConfirmacion(''); enviando.current = false; onBusy(false); }
  }
  return <form aria-label="Crear cuenta" onSubmit={e => { e.preventDefault(); void crear(); }}>
    <h3>Nueva cuenta</h3>
    <fieldset disabled={busy}><legend>Acceso inicial</legend>
      <label htmlFor="nueva-identidad">Identificador de la nueva cuenta</label>
      <input id="nueva-identidad" required maxLength={254} autoComplete="off" value={identificador} onChange={e => setIdentificador(e.target.value)} aria-describedby="formato-identificador" />
      <p id="formato-identificador" className="access-help">Letras, números, punto, guion, guion bajo o @, sin espacios. Se distinguen mayúsculas y minúsculas.</p>
      <label htmlFor="nueva-clave">Contraseña inicial</label>
      <input id="nueva-clave" type="password" autoComplete="new-password" required maxLength={256} value={contrasena} onChange={e => setContrasena(e.target.value)} aria-describedby="politica-inicial" />
      <p id="politica-inicial" className="access-help">Entre 12 y 128 caracteres. No se podrá consultar después de guardarla.</p>
      <label htmlFor="confirmar-clave">Confirmar contraseña inicial</label>
      <input id="confirmar-clave" type="password" autoComplete="new-password" required maxLength={256} value={confirmacion} onChange={e => setConfirmacion(e.target.value)} />
    </fieldset>
    <fieldset disabled={busy}><legend>Roles iniciales</legend>{(Object.keys(nombres) as Rol[]).map(rol => <label className="role-option" key={rol}><input type="checkbox" checked={roles.includes(rol)} onChange={e => setRoles(actuales => e.target.checked ? [...actuales, rol] : actuales.filter(r => r !== rol))} />{nombres[rol]}</label>)}</fieldset>
    <p className="access-help">La cuenta se creará activa. Sin roles podrá iniciar sesión, pero no realizar operaciones de negocio.</p>
    {error && <p role="alert" className="error">{error}</p>}
    <button className="primary-button" disabled={busy}>{busy ? 'Creando cuenta…' : 'Crear cuenta'}</button>
  </form>;
}
export function EditarCuentaForm({ cuenta, busy, onBusy, onSaved, onExpired }: Props & { cuenta: CuentaRoles }) {
  const [identificador, setIdentificador] = useState(cuenta.identificador);
  const [activa, setActiva] = useState(cuenta.activa);
  const [error, setError] = useState('');
  const [conflicto, setConflicto] = useState(false);
  const enviando = useRef(false);
  async function guardar() {
    if (enviando.current || conflicto) return;
    enviando.current = true; onBusy(true); setError('');
    try { await onSaved(await cuentasApi.actualizar(cuenta, identificador.trim(), activa)); }
    catch (e) {
      if (e instanceof ApiError && e.status === 401) onExpired();
      else { setError(errorCuenta(e)); setConflicto(e instanceof ApiError && e.code === 'CONFLICTO'); }
    } finally { enviando.current = false; onBusy(false); }
  }
  return <form aria-label="Editar cuenta" onSubmit={e => { e.preventDefault(); void guardar(); }}>
    <h3>Datos de acceso</h3>
    <fieldset disabled={busy || conflicto}><legend>Identificador y estado</legend>
      <label htmlFor="editar-identidad">Identificador de acceso</label>
      <input id="editar-identidad" required maxLength={254} autoComplete="off" value={identificador} onChange={e => setIdentificador(e.target.value)} />
      <label className="role-option"><input type="checkbox" checked={activa} onChange={e => setActiva(e.target.checked)} />Cuenta activa</label>
    </fieldset>
    {!activa && cuenta.activa && <p className="notice">Al guardar, la cuenta perderá el acceso y sus sesiones abiertas dejarán de ser válidas. Su historial se conservará.</p>}
    {activa && !cuenta.activa && <p className="notice">La persona deberá iniciar sesión nuevamente; sus sesiones anteriores no se recuperan.</p>}
    {error && <p className="error" role="alert">{error}</p>}
    <button className="primary-button" disabled={busy || conflicto || (identificador.trim() === cuenta.identificador && activa === cuenta.activa)}>{busy ? 'Guardando cuenta…' : 'Guardar cuenta'}</button>
  </form>;
}
