import { useRef, useState } from 'react';
import { ApiError } from '../../../shared/api/http';
import { serviciosApi, type Servicio } from '../api/servicios';

type Fila = { clave: string; id: string | null; nombre: string; conCargo: boolean; tarifa: string; activa: boolean };
let siguienteClave = 0;
const nuevaFila = (): Fila => ({ clave: `nueva-${siguienteClave++}`, id: null, nombre: '', conCargo: true, tarifa: '', activa: true });

export function errorServicio(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'NOMBRE_DUPLICADO') return 'Ya existe un servicio con ese nombre.';
    if (error.code === 'CONFLICTO') return 'Otra persona cambió este servicio. Recárgalo antes de guardar.';
    if (error.code === 'MODALIDAD_INVALIDA') return 'Las modalidades guardadas no se pueden quitar; desactívalas.';
    if (error.code === 'NO_EXISTE') return 'El servicio ya no existe.';
    if (error.status === 403) return 'No tienes permiso para gestionar servicios.';
  }
  return error instanceof Error ? error.message : 'No se pudo guardar el servicio.';
}

type Props = { servicio: Servicio | null; onSaved: (servicio: Servicio) => void; onReload: () => void; onExpired: () => void };

/** Alta (servicio null) o edición. Las modalidades guardadas solo se desactivan; nunca se quitan. */
export function ServicioForm({ servicio, onSaved, onReload, onExpired }: Props) {
  const [nombre, setNombre] = useState(servicio?.nombre ?? '');
  const [duracion, setDuracion] = useState(servicio ? String(servicio.duracionMinutos) : '');
  const [activo, setActivo] = useState(servicio?.activo ?? true);
  const [filas, setFilas] = useState<Fila[]>(() => servicio
    ? servicio.modalidades.map(m => ({ clave: m.id, id: m.id, nombre: m.nombre, conCargo: m.conCargo, tarifa: m.tarifa.toFixed(2), activa: m.activa }))
    : [nuevaFila()]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [conflicto, setConflicto] = useState(false);
  const enviando = useRef(false);
  const cambiar = (clave: string, cambio: Partial<Fila>) => setFilas(actuales => actuales.map(f => f.clave === clave ? { ...f, ...cambio } : f));

  async function guardar() {
    if (enviando.current) return;
    enviando.current = true; setBusy(true); setError('');
    const datos = {
      nombre: nombre.trim(),
      duracionMinutos: Number(duracion),
      modalidades: filas.map(f => ({ id: f.id, nombre: f.nombre.trim(), conCargo: f.conCargo, tarifa: f.conCargo ? f.tarifa.trim() : '0', activa: f.activa })),
    };
    try {
      onSaved(servicio ? await serviciosApi.actualizar(servicio, { ...datos, activo }) : await serviciosApi.crear(datos));
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) onExpired();
      else {
        if (e instanceof ApiError && e.code === 'CONFLICTO') setConflicto(true);
        setError(errorServicio(e) + (e instanceof ApiError && e.status === 0 && !servicio ? ' Antes de repetir el alta, recarga la lista para comprobar si se creó.' : ''));
      }
    } finally { enviando.current = false; setBusy(false); }
  }

  const titulo = servicio ? `Editar ${servicio.nombre}` : 'Nuevo servicio';
  return <form aria-label={titulo} onSubmit={e => { e.preventDefault(); void guardar(); }}>
    <h3>{titulo}</h3>
    <fieldset disabled={busy || conflicto}><legend>Servicio</legend>
      <label htmlFor="servicio-nombre">Nombre del servicio</label>
      <input id="servicio-nombre" required maxLength={100} autoComplete="off" value={nombre} onChange={e => setNombre(e.target.value)} />
      <label htmlFor="servicio-duracion">Duración en minutos</label>
      <input id="servicio-duracion" type="number" required min={1} max={480} step={1} value={duracion} onChange={e => setDuracion(e.target.value)} aria-describedby="servicio-duracion-ayuda" />
      <p id="servicio-duracion-ayuda" className="access-help">Entre 1 y 480 minutos. Cambiarla no modifica citas ya reservadas.</p>
      {servicio && <label className="role-option"><input type="checkbox" checked={activo} onChange={e => setActivo(e.target.checked)} />Servicio activo</label>}
      {servicio && !activo && <p className="notice">Un servicio inactivo no podrá recibir nuevas reservas.</p>}
    </fieldset>
    {filas.map((fila, indice) => <fieldset key={fila.clave} disabled={busy || conflicto}>
      <legend>Modalidad {indice + 1}</legend>
      <label htmlFor={`modalidad-nombre-${fila.clave}`}>Nombre de la modalidad</label>
      <input id={`modalidad-nombre-${fila.clave}`} required maxLength={60} autoComplete="off" value={fila.nombre} onChange={e => cambiar(fila.clave, { nombre: e.target.value })} />
      <label className="role-option"><input type="checkbox" checked={fila.conCargo} onChange={e => cambiar(fila.clave, { conCargo: e.target.checked, tarifa: e.target.checked ? '' : '0.00' })} />Con cargo</label>
      <label htmlFor={`modalidad-tarifa-${fila.clave}`}>Tarifa en soles (S/)</label>
      <input id={`modalidad-tarifa-${fila.clave}`} inputMode="decimal" required={fila.conCargo} disabled={!fila.conCargo} pattern="[0-9]{1,8}(\.[0-9]{1,2})?"
        value={fila.conCargo ? fila.tarifa : '0.00'} onChange={e => cambiar(fila.clave, { tarifa: e.target.value })} aria-describedby={`modalidad-tarifa-ayuda-${fila.clave}`} />
      <p id={`modalidad-tarifa-ayuda-${fila.clave}`} className="access-help">{fila.conCargo ? 'Mayor que cero, hasta 2 decimales. Ej.: 40.50' : 'Sin cargo: el importe es S/ 0.00.'}</p>
      <label className="role-option"><input type="checkbox" checked={fila.activa} onChange={e => cambiar(fila.clave, { activa: e.target.checked })} />Modalidad activa</label>
      {fila.id === null && filas.length > 1 && <button type="button" onClick={() => setFilas(actuales => actuales.filter(f => f.clave !== fila.clave))}>Quitar modalidad {indice + 1}</button>}
    </fieldset>)}
    <button type="button" disabled={busy || conflicto} onClick={() => setFilas(actuales => [...actuales, nuevaFila()])}>Agregar modalidad</button>
    <p className="access-help">Cambiar una tarifa no modifica citas ni cargos ya registrados. Las modalidades guardadas se desactivan en lugar de quitarse.</p>
    {error && <p role="alert" className="error">{error}</p>}
    {conflicto && <button type="button" onClick={onReload}>Recargar servicio</button>}
    <button className="primary-button" disabled={busy || conflicto}>{busy ? 'Guardando servicio…' : servicio ? 'Guardar servicio' : 'Crear servicio'}</button>
  </form>;
}
