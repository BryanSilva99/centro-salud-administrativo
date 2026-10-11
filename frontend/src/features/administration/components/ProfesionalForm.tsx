import { useEffect, useRef, useState } from 'react';
import { ApiError } from '../../../shared/api/http';
import { profesionalesApi, type FichaProfesional } from '../api/profesionales';
import { serviciosApi, type Servicio } from '../api/servicios';

export function errorProfesional(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'CONFLICTO') return 'Otra persona cambió esta ficha. Recárgala antes de guardar.';
    if (error.code === 'SERVICIO_INVALIDO') return 'Selecciona un servicio existente.';
    if (error.code === 'NO_EXISTE') return 'La ficha ya no existe.';
    if (error.status === 409) return 'Ya existe una ficha con esa identificación.';
    if (error.status === 403) return 'No tienes permiso para gestionar profesionales.';
  }
  return error instanceof Error ? error.message : 'No se pudo guardar la ficha.';
}

type Props = { ficha: FichaProfesional | null; onSaved: (ficha: FichaProfesional) => void; onReload: () => void; onExpired: () => void };

/** Alta (ficha null) o edición. Los servicios habilitados se reemplazan por completo en cada guardado. */
export function ProfesionalForm({ ficha, onSaved, onReload, onExpired }: Props) {
  const [identificacion, setIdentificacion] = useState(ficha?.identificacion ?? '');
  const [nombre, setNombre] = useState(ficha?.nombre ?? '');
  const [funcion, setFuncion] = useState(ficha?.funcion ?? '');
  const [activa, setActiva] = useState(ficha?.activa ?? true);
  const [seleccionados, setSeleccionados] = useState<string[]>(ficha?.serviciosHabilitados ?? []);
  const [servicios, setServicios] = useState<Servicio[] | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [conflicto, setConflicto] = useState(false);
  const enviando = useRef(false);

  useEffect(() => {
    const controller = new AbortController();
    serviciosApi.listar(controller.signal).then(s => { if (!controller.signal.aborted) setServicios(s); }).catch(() => {});
    return () => controller.abort();
  }, []);

  function alternar(servicioId: string) {
    setSeleccionados(actuales => actuales.includes(servicioId) ? actuales.filter(id => id !== servicioId) : [...actuales, servicioId]);
  }

  async function guardar() {
    if (enviando.current) return;
    enviando.current = true; setBusy(true); setError('');
    const datos = { identificacion: identificacion.trim(), nombre: nombre.trim(), funcion: funcion.trim(), serviciosHabilitados: seleccionados };
    try {
      onSaved(ficha ? await profesionalesApi.actualizar(ficha, { ...datos, activa }) : await profesionalesApi.crear(datos));
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) onExpired();
      else {
        if (e instanceof ApiError && e.code === 'CONFLICTO') setConflicto(true);
        setError(errorProfesional(e) + (e instanceof ApiError && e.status === 0 && !ficha ? ' Antes de repetir el alta, recarga la lista para comprobar si se creó.' : ''));
      }
    } finally { enviando.current = false; setBusy(false); }
  }

  const titulo = ficha ? `Editar ${ficha.identificacion}` : 'Nueva ficha profesional';
  return <form aria-label={titulo} onSubmit={e => { e.preventDefault(); void guardar(); }}>
    <h3>{titulo}</h3>
    <fieldset disabled={busy || conflicto}><legend>Profesional</legend>
      <label htmlFor="profesional-identificacion">Identificación</label>
      <input id="profesional-identificacion" required maxLength={50} autoComplete="off" value={identificacion} onChange={e => setIdentificacion(e.target.value)} />
      <label htmlFor="profesional-nombre">Nombre completo</label>
      <input id="profesional-nombre" required maxLength={150} autoComplete="off" value={nombre} onChange={e => setNombre(e.target.value)} />
      <label htmlFor="profesional-funcion">Especialidad o función</label>
      <input id="profesional-funcion" required maxLength={100} autoComplete="off" value={funcion} onChange={e => setFuncion(e.target.value)} />
      {ficha && <label className="role-option"><input type="checkbox" checked={activa} onChange={e => setActiva(e.target.checked)} />Ficha activa</label>}
      {ficha && !activa && <p className="notice">Un profesional inactivo no podrá recibir nuevas reservas.</p>}
    </fieldset>
    <fieldset disabled={busy || conflicto}><legend>Servicios habilitados</legend>
      {servicios === null && <p role="status">Cargando servicios…</p>}
      {servicios?.length === 0 && <p className="notice">Todavía no hay servicios registrados.</p>}
      {servicios?.map(s => <label className="role-option" key={s.id}>
        <input type="checkbox" checked={seleccionados.includes(s.id)} onChange={() => alternar(s.id)} />{s.nombre}
      </label>)}
    </fieldset>
    {error && <p role="alert" className="error">{error}</p>}
    {conflicto && <button type="button" onClick={onReload}>Recargar ficha</button>}
    <button className="primary-button" disabled={busy || conflicto}>{busy ? 'Guardando ficha…' : ficha ? 'Guardar ficha' : 'Crear ficha'}</button>
  </form>;
}
