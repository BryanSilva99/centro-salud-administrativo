import { useCallback, useEffect, useRef, useState } from 'react';
import { auth, type Cuenta } from '../api/auth';
import { ApiError } from '../../../shared/api/http';
import { useSessionActivity } from '../hooks/useSessionActivity';

export function SessionView({ cuenta, onExit, onOpenBilling }: { cuenta: Cuenta; onExit: (message: string) => void; onOpenBilling?: () => void }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [connected, setConnected] = useState(true);
  const title = useRef<HTMLHeadingElement>(null);
  const expired = useCallback(() => onExit('Tu sesión terminó. Vuelve a ingresar para continuar.'), [onExit]);
  useSessionActivity(cuenta.id, expired, setConnected);
  useEffect(() => { title.current?.focus(); }, []);

  async function logout() {
    setBusy(true); setError('');
    try { await auth.logout(); onExit('Cerraste sesión correctamente.'); }
    catch (failure) {
      if (failure instanceof ApiError && failure.status === 401) expired();
      else setError(failure instanceof Error ? failure.message : 'No pudimos cerrar sesión. Vuelve a intentarlo.');
    } finally { setBusy(false); }
  }
  return <section className="access-card session-card" aria-labelledby="session-title">
    <span className="status-badge"><span aria-hidden="true">●</span> Sesión iniciada</span>
    <h1 id="session-title" tabIndex={-1} ref={title}>Hola, {cuenta.identificador}</h1>
    <p className="intro">Ya accediste al sistema del centro de salud.</p>
    <dl className="account-details"><div><dt>Cuenta</dt><dd>{cuenta.identificador}</dd></div><div><dt>Estado</dt><dd>Activa</dd></div></dl>
    <p className="notice">Las funciones administrativas estarán disponibles cuando se habiliten los módulos y permisos correspondientes.</p>
    {!connected && <p className="error" role="status">No pudimos comprobar la sesión. Revisa tu conexión; volveremos a comprobarla.</p>}
    {error && <p className="error" role="alert">{error}</p>}
    {onOpenBilling && (
      <button type="button" className="primary-button billing-button" data-no-activity onClick={onOpenBilling}>
        Ir a Caja
      </button>
    )}
    <button type="button" className="primary-button" data-no-activity disabled={busy} onClick={logout}>{busy ? 'Cerrando sesión…' : 'Cerrar sesión'}</button>
    <p className="access-help">Cierra tu sesión cuando termines, especialmente en equipos compartidos.</p>
  </section>;
}
