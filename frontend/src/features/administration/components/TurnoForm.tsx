import { useRef, useState } from 'react';
import { ApiError } from '../../../shared/api/http';
import { disponibilidadApi, type TipoRecurso, type Turno } from '../api/disponibilidad';

export function errorTurno(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'SUPERPUESTO') return 'El intervalo se cruza con otro turno activo de este recurso en esa fecha.';
    if (error.code === 'RECURSO_INVALIDO') return 'Selecciona un profesional o consultorio existente.';
    if (error.code === 'NO_EXISTE') return 'El turno ya no existe.';
    if (error.status === 403) return 'No tienes permiso para gestionar la disponibilidad.';
  }
  return error instanceof Error ? error.message : 'No se pudo guardar el turno.';
}

type Props = { tipo: TipoRecurso; recursoId: string; onSaved: (turno: Turno) => void; onExpired: () => void };

/** Alta de turno. Un turno no se edita: para cambiar un horario se desactiva y se crea uno nuevo. */
export function TurnoForm({ tipo, recursoId, onSaved, onExpired }: Props) {
  const hoy = new Date();
  const [fecha, setFecha] = useState(`${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, '0')}-${String(hoy.getDate()).padStart(2, '0')}`);
  const [horaInicio, setHoraInicio] = useState('08:00');
  const [horaFin, setHoraFin] = useState('12:00');
  const [tipoTurno, setTipoTurno] = useState<'DISPONIBLE' | 'DESCANSO'>('DISPONIBLE');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const enviando = useRef(false);

  async function guardar() {
    if (enviando.current) return;
    enviando.current = true; setBusy(true); setError('');
    try {
      const turno = await disponibilidadApi.crear(tipo, recursoId, { fecha, horaInicio, horaFin, tipo: tipoTurno });
      onSaved(turno);
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) onExpired();
      else setError(errorTurno(e));
    } finally { enviando.current = false; setBusy(false); }
  }

  return <form aria-label="Nuevo turno" onSubmit={e => { e.preventDefault(); void guardar(); }}>
    <h3>Nuevo turno</h3>
    <fieldset disabled={busy}><legend>Intervalo</legend>
      <label htmlFor="turno-fecha">Fecha</label>
      <input id="turno-fecha" type="date" required value={fecha} onChange={e => setFecha(e.target.value)} />
      <label htmlFor="turno-inicio">Hora de inicio</label>
      <input id="turno-inicio" type="time" required value={horaInicio} onChange={e => setHoraInicio(e.target.value)} />
      <label htmlFor="turno-fin">Hora de fin</label>
      <input id="turno-fin" type="time" required value={horaFin} onChange={e => setHoraFin(e.target.value)} />
      <label htmlFor="turno-tipo">Tipo</label>
      <select id="turno-tipo" value={tipoTurno} onChange={e => setTipoTurno(e.target.value as 'DISPONIBLE' | 'DESCANSO')}>
        <option value="DISPONIBLE">Disponible</option>
        <option value="DESCANSO">Descanso</option>
      </select>
      <p className="access-help">Dos turnos del mismo recurso y fecha no pueden superponerse, aunque sean de tipos distintos.</p>
    </fieldset>
    {error && <p role="alert" className="error">{error}</p>}
    <button className="primary-button" disabled={busy}>{busy ? 'Guardando turno…' : 'Crear turno'}</button>
  </form>;
}
