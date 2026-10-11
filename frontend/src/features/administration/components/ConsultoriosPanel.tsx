import { useEffect, useState } from 'react';
import { ApiError } from '../../../shared/api/http';
import { consultoriosApi, type Consultorio } from '../api/consultorios';
import { ConsultorioForm, errorConsultorio } from './ConsultorioForm';

const resumen = (c: Consultorio) => `${c.codigo} · ${c.nombre} · ${c.activo ? 'Activo' : 'Inactivo'}`;

export function ConsultoriosPanel({ onExpired }: { onExpired: () => void }) {
  const [lista, setLista] = useState<Consultorio[] | null>(null);
  // null: ninguno; 'nuevo': alta; id: edición del consultorio con ese id.
  const [seleccion, setSeleccion] = useState<string | null>(null);
  const [revision, setRevision] = useState(0);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    setLista(null); setError('');
    consultoriosApi.listar(controller.signal).then(c => { if (!controller.signal.aborted) setLista(c); }).catch(e => {
      if (controller.signal.aborted) return;
      if (e instanceof ApiError && e.status === 401) onExpired(); else setError(errorConsultorio(e));
    });
    return () => controller.abort();
  }, [revision, onExpired]);

  const seleccionado = lista?.find(c => c.id === seleccion) ?? null;
  function guardado(consultorio: Consultorio) {
    setNotice(`Consultorio "${consultorio.codigo}" guardado.`); setSeleccion(consultorio.id); setRevision(r => r + 1);
  }
  return <section className="roles-panel" aria-labelledby="consultorios-titulo">
    <h2 id="consultorios-titulo">Consultorios</h2>
    <button type="button" aria-pressed={seleccion === 'nuevo'} onClick={() => { setNotice(''); setSeleccion('nuevo'); }}>Nuevo consultorio</button>
    {lista === null && !error && <p role="status">Cargando consultorios…</p>}
    {lista?.length === 0 && <p className="notice">Todavía no hay consultorios registrados.</p>}
    {lista && lista.length > 0 && <ul className="account-picker" aria-label="Consultorios">
      {lista.map(c => <li key={c.id}><button type="button" aria-pressed={c.id === seleccion} onClick={() => { setNotice(''); setSeleccion(c.id); }}>{resumen(c)}</button></li>)}
    </ul>}
    {error && <p role="alert" className="error">{error}</p>}
    {notice && <p role="status" className="notice">{notice}</p>}
    {seleccion === 'nuevo' && <ConsultorioForm key={`nuevo-${revision}`} consultorio={null} onSaved={guardado} onReload={() => setRevision(r => r + 1)} onExpired={onExpired} />}
    {seleccionado && <ConsultorioForm key={`${seleccionado.id}-${seleccionado.version}-${revision}`} consultorio={seleccionado} onSaved={guardado} onReload={() => setRevision(r => r + 1)} onExpired={onExpired} />}
  </section>;
}
