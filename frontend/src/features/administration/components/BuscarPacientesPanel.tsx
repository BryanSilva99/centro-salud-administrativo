import { useEffect, useState } from 'react';
import { ApiError } from '../../../shared/api/http';
import { pacientesApi, type Paciente, type Pagina } from '../api/pacientes';
import { EditarPacienteForm, errorEdicionPaciente } from './EditarPacienteForm';

const vacia: Pagina<Paciente> = { elementos: [], hayMas: false };
const resumen = (p: Paciente) => `${p.codigo} · ${p.nombres} ${p.apellidos}`;

/** RF-05: búsqueda por código, documento o nombre, paginada, con edición conservando el código. */
export function BuscarPacientesPanel({ onExpired }: { onExpired: () => void }) {
  const [busqueda, setBusqueda] = useState('');
  const [consulta, setConsulta] = useState('');
  const [pagina, setPagina] = useState(0);
  const [lista, setLista] = useState<Pagina<Paciente>>(vacia);
  const [seleccionId, setSeleccionId] = useState('');
  const [revision, setRevision] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  function fallo(e: unknown) {
    if (e instanceof ApiError && e.status === 401) { onExpired(); return; }
    setError(errorEdicionPaciente(e));
  }
  useEffect(() => {
    const controller = new AbortController();
    setLista(vacia); setError('');
    pacientesApi.buscar(consulta, pagina, controller.signal).then(p => { if (!controller.signal.aborted) setLista(p); })
      .catch(e => { if (!controller.signal.aborted) fallo(e); });
    return () => controller.abort();
  }, [consulta, pagina, revision]);

  const seleccionado = lista.elementos.find(p => p.id === seleccionId) ?? null;
  function guardado(paciente: Paciente) {
    setNotice(`Paciente "${paciente.codigo}" actualizado.`); setSeleccionId(paciente.id); setRevision(r => r + 1);
  }
  return <section className="roles-panel" aria-labelledby="buscar-pacientes-titulo">
    <h2 id="buscar-pacientes-titulo">Buscar pacientes</h2>
    <form onSubmit={e => { e.preventDefault(); setConsulta(busqueda); setPagina(0); setSeleccionId(''); setNotice(''); }}>
      <label htmlFor="buscar-paciente">Documento, nombre o código</label>
      <input id="buscar-paciente" value={busqueda} maxLength={150} disabled={busy} onChange={e => setBusqueda(e.target.value)} />
      <button disabled={busy}>Buscar</button>
    </form>
    {lista.elementos.length === 0 && <p className="notice">Sin resultados en esta página.</p>}
    {lista.elementos.length > 0 && <ul className="account-picker" aria-label="Pacientes">
      {lista.elementos.map(p => <li key={p.id}>
        <button type="button" aria-pressed={p.id === seleccionId} onClick={() => { setSeleccionId(p.id); setNotice(''); }}>{resumen(p)}</button>
      </li>)}
    </ul>}
    <div className="pagination">
      <button disabled={busy || pagina === 0} onClick={() => setPagina(p => p - 1)}>Página anterior</button>
      <span>Página {pagina + 1}</span>
      <button disabled={busy || !lista.hayMas} onClick={() => setPagina(p => p + 1)}>Página siguiente</button>
    </div>
    {error && <p role="alert" className="error">{error}</p>}
    {notice && <p role="status" className="notice">{notice}</p>}
    {seleccionado && <EditarPacienteForm key={`${seleccionado.id}-${seleccionado.version}-${revision}`}
      paciente={seleccionado} onSaved={guardado} onReload={() => setRevision(r => r + 1)} onExpired={onExpired} />}
  </section>;
}
