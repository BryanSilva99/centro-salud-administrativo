import { useEffect, useState } from 'react';
import { ApiError } from '../../../shared/api/http';
import { profesionalesApi, type FichaProfesional } from '../api/profesionales';
import { ProfesionalForm, errorProfesional } from './ProfesionalForm';

const resumen = (f: FichaProfesional) => `${f.identificacion} · ${f.nombre} · ${f.activa ? 'Activo' : 'Inactivo'}`;

export function ProfesionalesPanel({ onExpired }: { onExpired: () => void }) {
  const [lista, setLista] = useState<FichaProfesional[] | null>(null);
  // null: ninguno; 'nuevo': alta; id: edición de la ficha con ese id.
  const [seleccion, setSeleccion] = useState<string | null>(null);
  const [revision, setRevision] = useState(0);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    setLista(null); setError('');
    profesionalesApi.listar(controller.signal).then(f => { if (!controller.signal.aborted) setLista(f); }).catch(e => {
      if (controller.signal.aborted) return;
      if (e instanceof ApiError && e.status === 401) onExpired(); else setError(errorProfesional(e));
    });
    return () => controller.abort();
  }, [revision, onExpired]);

  const seleccionada = lista?.find(f => f.id === seleccion) ?? null;
  function guardada(ficha: FichaProfesional) {
    setNotice(`Ficha "${ficha.identificacion}" guardada.`); setSeleccion(ficha.id); setRevision(r => r + 1);
  }
  return <section className="roles-panel" aria-labelledby="profesionales-titulo">
    <h2 id="profesionales-titulo">Profesionales</h2>
    <button type="button" aria-pressed={seleccion === 'nuevo'} onClick={() => { setNotice(''); setSeleccion('nuevo'); }}>Nueva ficha profesional</button>
    {lista === null && !error && <p role="status">Cargando profesionales…</p>}
    {lista?.length === 0 && <p className="notice">Todavía no hay fichas registradas.</p>}
    {lista && lista.length > 0 && <ul className="account-picker" aria-label="Profesionales">
      {lista.map(f => <li key={f.id}><button type="button" aria-pressed={f.id === seleccion} onClick={() => { setNotice(''); setSeleccion(f.id); }}>{resumen(f)}</button></li>)}
    </ul>}
    {error && <p role="alert" className="error">{error}</p>}
    {notice && <p role="status" className="notice">{notice}</p>}
    {seleccion === 'nuevo' && <ProfesionalForm key={`nuevo-${revision}`} ficha={null} onSaved={guardada} onReload={() => setRevision(r => r + 1)} onExpired={onExpired} />}
    {seleccionada && <ProfesionalForm key={`${seleccionada.id}-${seleccionada.version}-${revision}`} ficha={seleccionada} onSaved={guardada} onReload={() => setRevision(r => r + 1)} onExpired={onExpired} />}
  </section>;
}
