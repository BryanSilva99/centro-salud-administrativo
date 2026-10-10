import { useRef, useState, type FormEvent } from 'react';
import { auth, type Cuenta } from '../api/auth';

export function LoginForm({ onLogin, notice }: { onLogin: (cuenta: Cuenta) => void; notice: string }) {
  const [identificador, setIdentificador] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const inFlight = useRef(false);
  const errorRef = useRef<HTMLParagraphElement>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (inFlight.current) return;
    if (!identificador.trim() || !contrasena) { setError('Completa tu identificador y contraseña.'); return; }
    inFlight.current = true; setBusy(true); setError('');
    try {
      const cuenta = await auth.login(identificador, contrasena);
      setContrasena('');
      onLogin(cuenta);
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'No pudimos iniciar sesión. Inténtalo de nuevo.');
      setContrasena('');
      requestAnimationFrame(() => errorRef.current?.focus());
    } finally { inFlight.current = false; setBusy(false); }
  }

  return <section className="access-card" aria-labelledby="login-title">
    <p className="eyebrow">Acceso del personal</p>
    <h1 id="login-title">Bienvenido de nuevo</h1>
    <p className="intro">Ingresa con tu cuenta para acceder al sistema del centro de salud.</p>
    {notice && <p className="notice" role="status">{notice}</p>}
    <form onSubmit={submit} aria-busy={busy}>
      <label htmlFor="identificador">Identificador</label>
      <input id="identificador" name="identificador" autoComplete="username" autoCapitalize="none"
        spellCheck={false} required maxLength={254} value={identificador} disabled={busy}
        aria-describedby="identifier-help" onChange={e => setIdentificador(e.target.value)} />
      <p className="field-help" id="identifier-help">Usa el identificador asignado a tu cuenta.</p>
      <label htmlFor="contrasena">Contraseña</label>
      <div className="password-field">
        <input id="contrasena" name="contrasena" type={visible ? 'text' : 'password'} autoComplete="current-password"
          required maxLength={256} value={contrasena} disabled={busy} onChange={e => setContrasena(e.target.value)} />
        <button className="visibility-button" type="button" aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          aria-pressed={visible} disabled={busy} onClick={() => setVisible(!visible)}>{visible ? 'Ocultar' : 'Mostrar'}</button>
      </div>
      {error && <p className="error" role="alert" tabIndex={-1} ref={errorRef}>{error}</p>}
      <button className="primary-button" type="submit" disabled={busy}>{busy ? 'Ingresando…' : 'Iniciar sesión'}<span aria-hidden="true">→</span></button>
    </form>
    <p className="access-help">¿Necesitas acceso? Comunícate con la administración del centro.</p>
  </section>;
}
