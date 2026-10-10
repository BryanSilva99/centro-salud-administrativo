import { useCallback, useEffect, useState } from 'react';
import { auth, type Cuenta } from '../features/identity/api/auth';
import { LoginForm } from '../features/identity/components/LoginForm';
import { SessionView } from '../features/identity/components/SessionView';
import './styles.css';

type State = { kind: 'loading' } | { kind: 'unavailable' } | { kind: 'anonymous'; notice: string } | { kind: 'authenticated'; cuenta: Cuenta };

export function App() {
  const [state, setState] = useState<State>({ kind: 'loading' });
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setState({ kind: 'loading' });
    auth.me(controller.signal).then(cuenta => {
      if (!controller.signal.aborted) setState(cuenta ? { kind: 'authenticated', cuenta } : { kind: 'anonymous', notice: '' });
    }).catch(() => { if (!controller.signal.aborted) setState({ kind: 'unavailable' }); });
    return () => controller.abort();
  }, [retry]);
  const refresh = useCallback((cuenta: Cuenta) => setState({ kind: 'authenticated', cuenta }), []);
  const exit = useCallback((notice: string) => setState({ kind: 'anonymous', notice }), []);

  return <div className="app-shell">
    <header className="site-header"><a className="brand" href="/" aria-label="Centro de Salud, inicio"><span className="brand-mark" aria-hidden="true">+</span><span>Centro de Salud<small>Gestión administrativa</small></span></a><span className="header-caption">Portal del personal</span></header>
    <main id="main-content" className="main-layout">
      <aside className="welcome-panel" aria-label="Presentación del sistema">
        <p className="eyebrow">Al servicio de una mejor atención</p>
        <h2>El cuidado comienza<br />con una buena gestión.</h2>
        <p>Un espacio de trabajo para el personal que hace posible la atención de cada día.</p>
        <div className="illustration" aria-hidden="true"><div className="orbit orbit-one" /><div className="orbit orbit-two" /><div className="medical-symbol">+</div><span className="illustration-label">CENTRO DE SALUD</span></div>
        <p className="panel-footnote">Acceso exclusivo para personal autorizado.</p>
      </aside>
      <div className="form-panel">
        {state.kind === 'loading' && <section className="access-card"><h1>Comprobando sesión</h1><p role="status">Un momento, por favor…</p></section>}
        {state.kind === 'unavailable' && <section className="access-card"><h1>No pudimos conectar</h1><p className="intro" role="alert">Comprueba tu conexión e inténtalo nuevamente.</p><button className="primary-button" onClick={() => setRetry(value => value + 1)}>Volver a intentar</button></section>}
        {state.kind === 'anonymous' && <LoginForm notice={state.notice} onLogin={cuenta => setState({ kind: 'authenticated', cuenta })} />}
        {state.kind === 'authenticated' && <SessionView cuenta={state.cuenta} onExit={exit} onRefresh={refresh} />}
      </div>
    </main>
    <footer>Centro de Salud <span aria-hidden="true">·</span> Sistema de gestión administrativa</footer>
  </div>;
}
