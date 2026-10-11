import { useCallback, useEffect, useState } from 'react';
import { auth, type Cuenta } from '../features/identity/api/auth';
import { LoginForm } from '../features/identity/components/LoginForm';
import { SessionView } from '../features/identity/components/SessionView';
import { AppointmentsWorkspace } from '../features/appointments/components/AppointmentsWorkspace';
import { BillingView } from '../features/billing/components/BillingView';
import './styles.css';

type State = { kind: 'loading' } | { kind: 'unavailable' } | { kind: 'anonymous'; notice: string } | { kind: 'authenticated'; cuenta: Cuenta; verBilling: boolean };

export function App() {
  const [state, setState] = useState<State>({ kind: 'loading' });
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setState({ kind: 'loading' });
    auth.me(controller.signal).then(cuenta => {
      if (!controller.signal.aborted) setState(cuenta ? { kind: 'authenticated', cuenta, verBilling: false } : { kind: 'anonymous', notice: '' });
    }).catch(() => { if (!controller.signal.aborted) setState({ kind: 'unavailable' }); });
    return () => controller.abort();
  }, [retry]);
  const refresh = useCallback((cuenta: Cuenta) => setState({ kind: 'authenticated', cuenta, verBilling: false }), []);
  const exit = useCallback((notice: string) => setState({ kind: 'anonymous', notice }), []);

  if (state.kind === 'authenticated' && state.verBilling) return <BillingView />;
  if (state.kind === 'authenticated') return <SessionView cuenta={state.cuenta} onExit={exit} onRefresh={refresh} onOpenBilling={() => setState({ ...state, verBilling: true })} />;

  return <div className="app-shell">
    <header className="site-header"><a className="brand" href="/" aria-label="Centro de Salud, inicio"><span className="brand-mark" aria-hidden="true">+</span><span>Centro de Salud<small>Sistema Administrativo</small></span></a><span className="header-caption">Portal del personal</span></header>
    <main id="main-content" className="main-layout">
      <aside className="welcome-panel" aria-label="Presentación del sistema">
        <h2>Operación clara para cuidar mejor.</h2>
        <p>Una plataforma interna para coordinar admisión, agenda y gestión administrativa del centro.</p>
        <div className="illustration" aria-hidden="true"><div className="cross-background" /><div className="medical-symbol" /></div>
        <p className="panel-footnote">Hecho para equipos de recepción, caja, administración y profesionales.</p>
      </aside>
      <div className="form-panel">
        {state.kind === 'loading' && <section className="access-card"><h1>Comprobando sesión</h1><p role="status">Un momento, por favor…</p></section>}
        {state.kind === 'unavailable' && <section className="access-card"><h1>No pudimos conectar</h1><p className="intro" role="alert">Comprueba tu conexión e inténtalo nuevamente.</p><button className="primary-button" onClick={() => setRetry(value => value + 1)}>Volver a intentar</button></section>}
        {state.kind === 'anonymous' && <LoginForm notice={state.notice} onLogin={cuenta => setState({ kind: 'authenticated', cuenta, verBilling: false })} />}
      </div>
    </main>
    <footer>Privacidad y acceso · Sesión protegida</footer>
  </div>;
}
