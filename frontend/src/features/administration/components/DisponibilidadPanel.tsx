import { useEffect, useState } from 'react';
import { ApiError } from '../../../shared/api/http';
import { disponibilidadApi, type TipoRecurso, type Turno } from '../api/disponibilidad';
import { profesionalesApi } from '../api/profesionales';
import { consultoriosApi } from '../api/consultorios';
import { errorTurno, TurnoForm } from './TurnoForm';

type Recurso = { id: string; etiqueta: string };

async function cargarRecursos(tipo: TipoRecurso, signal: AbortSignal): Promise<Recurso[]> {
  if (tipo === 'profesionales') {
    return (await profesionalesApi.listar(signal)).map(f => ({ id: f.id, etiqueta: `${f.identificacion} · ${f.nombre}` }));
  }
  return (await consultoriosApi.listar(signal)).map(c => ({ id: c.id, etiqueta: `${c.codigo} · ${c.nombre}` }));
}
const resumenTurno = (t: Turno) => `${t.fecha} · ${t.horaInicio}–${t.horaFin} · ${t.tipo === 'DISPONIBLE' ? 'Disponible' : 'Descanso'} · ${t.activo ? 'Activo' : 'Inactivo'}`;

export function DisponibilidadPanel({ onExpired }: { onExpired: () => void }) {
  const [tipo, setTipo] = useState<TipoRecurso>('profesionales');
  const [recursos, setRecursos] = useState<Recurso[] | null>(null);
  const [recursoId, setRecursoId] = useState('');
  const [turnos, setTurnos] = useState<Turno[] | null>(null);
  const [revision, setRevision] = useState(0);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    setRecursos(null); setRecursoId(''); setTurnos(null); setError('');
    cargarRecursos(tipo, controller.signal).then(r => { if (!controller.signal.aborted) setRecursos(r); }).catch(e => {
      if (controller.signal.aborted) return;
      if (e instanceof ApiError && e.status === 401) onExpired(); else setError(errorTurno(e));
    });
    return () => controller.abort();
  }, [tipo, onExpired]);

  useEffect(() => {
    if (!recursoId) return;
    const controller = new AbortController();
    setTurnos(null); setNotice('');
    disponibilidadApi.listar(tipo, recursoId, controller.signal).then(t => { if (!controller.signal.aborted) setTurnos(t); }).catch(e => {
      if (controller.signal.aborted) return;
      if (e instanceof ApiError && e.status === 401) onExpired(); else setError(errorTurno(e));
    });
    return () => controller.abort();
  }, [tipo, recursoId, revision, onExpired]);

  async function desactivar(turnoId: string) {
    setError('');
    try { await disponibilidadApi.desactivar(tipo, recursoId, turnoId); setNotice('Turno desactivado.'); setRevision(r => r + 1); }
    catch (e) {
      if (e instanceof ApiError && e.status === 401) onExpired(); else setError(errorTurno(e));
    }
  }

  return <section className="roles-panel" aria-labelledby="disponibilidad-titulo">
    <h2 id="disponibilidad-titulo">Disponibilidad</h2>
    <fieldset><legend>Recurso</legend>
      <label htmlFor="disponibilidad-tipo">Tipo de recurso</label>
      <select id="disponibilidad-tipo" value={tipo} onChange={e => setTipo(e.target.value as TipoRecurso)}>
        <option value="profesionales">Profesional</option>
        <option value="consultorios">Consultorio</option>
      </select>
      <label htmlFor="disponibilidad-recurso">Recurso</label>
      <select id="disponibilidad-recurso" value={recursoId} onChange={e => { setNotice(''); setRecursoId(e.target.value); }}>
        <option value="">Selecciona un recurso…</option>
        {recursos?.map(r => <option key={r.id} value={r.id}>{r.etiqueta}</option>)}
      </select>
      {recursos === null && <p role="status">Cargando recursos…</p>}
      {recursos?.length === 0 && <p className="notice">Todavía no hay recursos registrados.</p>}
    </fieldset>
    {error && <p role="alert" className="error">{error}</p>}
    {notice && <p role="status" className="notice">{notice}</p>}
    {recursoId && <>
      {turnos === null && <p role="status">Cargando turnos…</p>}
      {turnos?.length === 0 && <p className="notice">Este recurso todavía no tiene turnos.</p>}
      {turnos && turnos.length > 0 && <ul className="account-picker" aria-label="Turnos">
        {turnos.map(t => <li key={t.id}>
          {resumenTurno(t)}{t.activo && <button type="button" onClick={() => desactivar(t.id)}>Desactivar</button>}
        </li>)}
      </ul>}
      <TurnoForm key={`${tipo}-${recursoId}-${revision}`} tipo={tipo} recursoId={recursoId}
        onSaved={() => { setNotice('Turno creado.'); setRevision(r => r + 1); }} onExpired={onExpired} />
    </>}
  </section>;
}
