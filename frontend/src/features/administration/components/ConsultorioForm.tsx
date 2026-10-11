import { useEffect, useRef, useState } from 'react';
import { ApiError } from '../../../shared/api/http';
import { consultoriosApi, type Consultorio } from '../api/consultorios';
import { serviciosApi, type Servicio } from '../api/servicios';

export function errorConsultorio(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'CODIGO_DUPLICADO') return 'Ya existe un consultorio con ese código.';
    if (error.code === 'CONFLICTO') return 'Otra persona cambió este consultorio. Recárgalo antes de guardar.';
    if (error.code === 'SERVICIO_INVALIDO') return 'Selecciona un servicio existente.';
    if (error.code === 'NO_EXISTE') return 'El consultorio ya no existe.';
    if (error.status === 403) return 'No tienes permiso para gestionar consultorios.';
  }
  return error instanceof Error ? error.message : 'No se pudo guardar el consultorio.';
}

type Props = { consultorio: Consultorio | null; onSaved: (consultorio: Consultorio) => void; onReload: () => void; onExpired: () => void };

/** Alta (consultorio null) o edición. Los servicios compatibles se reemplazan por completo en cada guardado. */
export function ConsultorioForm({ consultorio, onSaved, onReload, onExpired }: Props) {
  const [codigo, setCodigo] = useState(consultorio?.codigo ?? '');
  const [nombre, setNombre] = useState(consultorio?.nombre ?? '');
  const [activo, setActivo] = useState(consultorio?.activo ?? true);
  const [seleccionados, setSeleccionados] = useState<string[]>(consultorio?.serviciosCompatibles ?? []);
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
    const datos = { codigo: codigo.trim(), nombre: nombre.trim(), serviciosCompatibles: seleccionados };
    try {
      onSaved(consultorio ? await consultoriosApi.actualizar(consultorio, { ...datos, activo }) : await consultoriosApi.crear(datos));
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) onExpired();
      else {
        if (e instanceof ApiError && e.code === 'CONFLICTO') setConflicto(true);
        setError(errorConsultorio(e) + (e instanceof ApiError && e.status === 0 && !consultorio ? ' Antes de repetir el alta, recarga la lista para comprobar si se creó.' : ''));
      }
    } finally { enviando.current = false; setBusy(false); }
  }

  const titulo = consultorio ? `Editar ${consultorio.codigo}` : 'Nuevo consultorio';
  return <form aria-label={titulo} onSubmit={e => { e.preventDefault(); void guardar(); }}>
    <h3>{titulo}</h3>
    <fieldset disabled={busy || conflicto}><legend>Consultorio</legend>
      <label htmlFor="consultorio-codigo">Código del consultorio</label>
      <input id="consultorio-codigo" required maxLength={20} autoComplete="off" value={codigo} onChange={e => setCodigo(e.target.value)} />
      <label htmlFor="consultorio-nombre">Nombre</label>
      <input id="consultorio-nombre" required maxLength={150} autoComplete="off" value={nombre} onChange={e => setNombre(e.target.value)} />
      {consultorio && <label className="role-option"><input type="checkbox" checked={activo} onChange={e => setActivo(e.target.checked)} />Consultorio activo</label>}
      {consultorio && !activo && <p className="notice">Un consultorio inactivo no podrá recibir nuevas reservas.</p>}
    </fieldset>
    <fieldset disabled={busy || conflicto}><legend>Servicios compatibles</legend>
      {servicios === null && <p role="status">Cargando servicios…</p>}
      {servicios?.length === 0 && <p className="notice">Todavía no hay servicios registrados.</p>}
      {servicios?.map(s => <label className="role-option" key={s.id}>
        <input type="checkbox" checked={seleccionados.includes(s.id)} onChange={() => alternar(s.id)} />{s.nombre}
      </label>)}
    </fieldset>
    {error && <p role="alert" className="error">{error}</p>}
    {conflicto && <button type="button" onClick={onReload}>Recargar consultorio</button>}
    <button className="primary-button" disabled={busy || conflicto}>{busy ? 'Guardando consultorio…' : consultorio ? 'Guardar consultorio' : 'Crear consultorio'}</button>
  </form>;
}
