import { useEffect, useState } from 'react';
import { ApiError } from '../../../shared/api/http';
import { serviciosApi, type Servicio } from '../api/servicios';
import { ServicioForm, errorServicio } from './ServicioForm';

const resumen = (s: Servicio) => `${s.nombre} · ${s.duracionMinutos} min · ${s.activo ? 'Activo' : 'Inactivo'}`;

export function ServiciosPanel({ onExpired }: { onExpired: () => void }) {
  const [lista, setLista] = useState<Servicio[] | null>(null);
  // null: ninguno; 'nuevo': alta; id: edición del servicio con ese id.
  const [seleccion, setSeleccion] = useState<string | null>(null);
  const [revision, setRevision] = useState(0);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    setLista(null); setError('');
    serviciosApi.listar(controller.signal).then(s => { if (!controller.signal.aborted) setLista(s); }).catch(e => {
      if (controller.signal.aborted) return;
      if (e instanceof ApiError && e.status === 401) onExpired(); else setError(errorServicio(e));
    });
    return () => controller.abort();
  }, [revision, onExpired]);

  const seleccionado = lista?.find(s => s.id === seleccion) ?? null;
  function guardado(servicio: Servicio) {
    setNotice(`Servicio "${servicio.nombre}" guardado.`); setSeleccion(servicio.id); setRevision(r => r + 1);
  }
  return <section className="roles-panel" aria-labelledby="servicios-titulo">
    <h2 id="servicios-titulo">Servicios y condiciones económicas</h2>
    <button type="button" aria-pressed={seleccion === 'nuevo'} onClick={() => { setNotice(''); setSeleccion('nuevo'); }}>Nuevo servicio</button>
    {lista === null && !error && <p role="status">Cargando servicios…</p>}
    {lista?.length === 0 && <p className="notice">Todavía no hay servicios registrados.</p>}
    {lista && lista.length > 0 && <ul className="account-picker" aria-label="Servicios">
      {lista.map(s => <li key={s.id}><button type="button" aria-pressed={s.id === seleccion} onClick={() => { setNotice(''); setSeleccion(s.id); }}>{resumen(s)}</button></li>)}
    </ul>}
    {error && <p role="alert" className="error">{error}</p>}
    {notice && <p role="status" className="notice">{notice}</p>}
    {seleccion === 'nuevo' && <ServicioForm key={`nuevo-${revision}`} servicio={null} onSaved={guardado} onReload={() => setRevision(r => r + 1)} onExpired={onExpired} />}
    {seleccionado && <ServicioForm key={`${seleccionado.id}-${seleccionado.version}-${revision}`} servicio={seleccionado} onSaved={guardado} onReload={() => setRevision(r => r + 1)} onExpired={onExpired} />}
  </section>;
}
