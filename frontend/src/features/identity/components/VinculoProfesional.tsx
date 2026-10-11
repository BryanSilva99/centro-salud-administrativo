import { useEffect, useState } from 'react';
import { profesionalesApi, type FichaProfesional } from '../api/profesionales';
import type { CuentaRoles } from '../api/roles';
import { ApiError } from '../../../shared/api/http';
import { errorCuenta } from './FormulariosCuenta';

export function VinculoProfesional({ cuenta, busy, onBusy, onSaved, onExpired }: {
  cuenta: CuentaRoles; busy: boolean; onBusy: (busy: boolean) => void;
  onSaved: (cuenta: CuentaRoles) => Promise<void>; onExpired: () => void;
}) {
  const [fichas, setFichas] = useState<FichaProfesional[]>([]);
  const [actual, setActual] = useState<FichaProfesional | null>(null);
  const [seleccion, setSeleccion] = useState(cuenta.profesionalId ?? '');
  const [busqueda, setBusqueda] = useState('');
  const [consulta, setConsulta] = useState('');
  const [recarga, setRecarga] = useState(0);
  const [error, setError] = useState('');
  const [conflicto, setConflicto] = useState(false);
  const [nueva, setNueva] = useState(false);
  const [identificacion, setIdentificacion] = useState('');
  const [nombre, setNombre] = useState('');
  const [funcion, setFuncion] = useState('');

  function mostrarError(error: unknown) {
    if (error instanceof ApiError && error.status === 401) { onExpired(); return; }
    setConflicto(error instanceof ApiError && error.code === 'CONFLICTO');
    setError(errorCuenta(error));
  }
  useEffect(() => {
    const controller = new AbortController();
    profesionalesApi.buscar(consulta, controller.signal).then(fichas => {
      if (!controller.signal.aborted) setFichas(fichas);
    }).catch(error => { if (!controller.signal.aborted) mostrarError(error); });
    return () => controller.abort();
  }, [consulta, recarga]);
  useEffect(() => {
    if (!cuenta.profesionalId) return;
    const controller = new AbortController();
    profesionalesApi.consultar(cuenta.profesionalId, controller.signal).then(ficha => {
      if (!controller.signal.aborted) setActual(ficha);
    }).catch(error => { if (!controller.signal.aborted) mostrarError(error); });
    return () => controller.abort();
  }, [cuenta.profesionalId]);

  async function guardarVinculo() {
    if (busy || conflicto) return;
    onBusy(true); setError('');
    try { await onSaved(await profesionalesApi.vincular(cuenta, seleccion || null)); }
    catch (error) { mostrarError(error); }
    finally { onBusy(false); }
  }
  async function registrarFicha() {
    if (busy) return;
    onBusy(true); setError('');
    try {
      const ficha = await profesionalesApi.registrar(identificacion.trim(), nombre.trim(), funcion.trim());
      setFichas([ficha]); setActual(ficha); setSeleccion(ficha.id); setNueva(false);
      setIdentificacion(''); setNombre(''); setFuncion('');
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) setError('Ya existe una ficha con esa identificación. Búscala para seleccionarla.');
      else mostrarError(error);
    } finally { onBusy(false); }
  }
  const opciones = actual && !fichas.some(ficha => ficha.id === actual.id) ? [actual, ...fichas] : fichas;
  return <section className="panel-card" aria-label="Vínculo profesional">
    <h2>Ficha profesional</h2>
    <p>Selecciona la ficha que corresponde a esta cuenta. Para cambiarla o retirarla, guarda el vínculo.</p>
    {error && <p role="alert" className="error">{error}</p>}
    <form onSubmit={event => { event.preventDefault(); setConsulta(busqueda); setRecarga(valor => valor + 1); }}>
      <label htmlFor="buscar-profesional">Buscar por nombre o identificación</label>
      <input id="buscar-profesional" maxLength={150} value={busqueda} disabled={busy} onChange={event => setBusqueda(event.target.value)} />
      <button disabled={busy}>Buscar ficha</button>
    </form>
    <form onSubmit={event => { event.preventDefault(); void guardarVinculo(); }}>
      <label htmlFor="ficha-profesional">Ficha vinculada</label>
      <select id="ficha-profesional" value={seleccion} disabled={busy || conflicto} onChange={event => setSeleccion(event.target.value)}>
        <option value="">Sin ficha vinculada</option>
        {seleccion && !opciones.some(ficha => ficha.id === seleccion) && <option value={seleccion}>Ficha actual: {seleccion}</option>}
        {opciones.map(ficha => <option key={ficha.id} value={ficha.id} disabled={!ficha.activa}>{ficha.nombre} · {ficha.identificacion}{!ficha.activa ? ' (inactiva)' : ''}</option>)}
      </select>
      <p className="access-help">Hasta 50 resultados. Refina la búsqueda si no encuentras la ficha.</p>
      <button className="primary-button" disabled={busy || conflicto || seleccion === (cuenta.profesionalId ?? '')}>Guardar vínculo</button>
    </form>
    <button disabled={busy} onClick={() => setNueva(valor => !valor)}>{nueva ? 'Cancelar nueva ficha' : 'Registrar ficha básica'}</button>
    {nueva && <form aria-label="Registrar ficha profesional" onSubmit={event => { event.preventDefault(); void registrarFicha(); }}>
      <fieldset disabled={busy}><legend>Identificación del profesional</legend>
        <label htmlFor="prof-identificacion">Identificación profesional</label>
        <input id="prof-identificacion" required maxLength={50} value={identificacion} onChange={event => setIdentificacion(event.target.value)} />
        <label htmlFor="prof-nombre">Nombre completo</label>
        <input id="prof-nombre" required maxLength={150} value={nombre} onChange={event => setNombre(event.target.value)} />
        <label htmlFor="prof-funcion">Especialidad o función</label>
        <input id="prof-funcion" required maxLength={100} value={funcion} onChange={event => setFuncion(event.target.value)} />
      </fieldset>
      <p className="access-help">Se guardará una ficha activa. Después pulsa Guardar vínculo para asociarla a la cuenta.</p>
      <button disabled={busy}>Guardar ficha</button>
    </form>}
  </section>;
}
