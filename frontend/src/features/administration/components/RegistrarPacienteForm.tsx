import { useRef, useState, type ChangeEvent } from 'react';
import { ApiError } from '../../../shared/api/http';
import { pacientesApi, type Paciente, type TipoDocumento } from '../api/pacientes';

const formatos: Record<TipoDocumento, { nombre: string; ayuda: string; patron: string }> = {
  DNI: { nombre: 'DNI', ayuda: '8 dígitos.', patron: '[0-9]{8}' },
  CE: { nombre: 'Carné de extranjería', ayuda: '9 letras o números.', patron: '[A-Za-z0-9]{9}' },
};
const vacio = { numeroDocumento: '', nombres: '', apellidos: '', fechaNacimiento: '', telefono: '', correo: '' };

function hoyLocal(): string {
  const ahora = new Date();
  return `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}-${String(ahora.getDate()).padStart(2, '0')}`;
}
function errorPaciente(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'DOCUMENTO_DUPLICADO') return 'Ya existe un paciente con ese documento. Utiliza el registro existente.';
    if (error.status === 403) return 'No tienes permiso para registrar pacientes.';
    if (error.status === 0) return `${error.message} Antes de repetir el registro, confirma con el equipo si el paciente ya fue registrado.`;
  }
  return error instanceof Error ? error.message : 'No se pudo registrar el paciente.';
}

export function RegistrarPacienteForm({ onExpired }: { onExpired: () => void }) {
  const [tipo, setTipo] = useState<TipoDocumento>('DNI');
  const [datos, setDatos] = useState(vacio);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [registrado, setRegistrado] = useState<Paciente | null>(null);
  const enviando = useRef(false);
  const campo = (nombre: keyof typeof vacio) => ({
    value: datos[nombre],
    onChange: (e: ChangeEvent<HTMLInputElement>) => setDatos(actual => ({ ...actual, [nombre]: e.target.value })),
  });

  async function registrar() {
    if (enviando.current) return;
    enviando.current = true; setBusy(true); setError(''); setRegistrado(null);
    try {
      const paciente = await pacientesApi.registrar({
        tipoDocumento: tipo,
        numeroDocumento: datos.numeroDocumento.trim(),
        nombres: datos.nombres.trim(),
        apellidos: datos.apellidos.trim(),
        fechaNacimiento: datos.fechaNacimiento,
        telefono: datos.telefono.trim() || null,
        correo: datos.correo.trim() || null,
      });
      setRegistrado(paciente); setDatos(vacio);
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) onExpired();
      else setError(errorPaciente(e));
    } finally { enviando.current = false; setBusy(false); }
  }

  return <section className="roles-panel" aria-labelledby="registrar-paciente">
    <h2 id="registrar-paciente">Registrar paciente</h2>
    <form aria-label="Registrar paciente" onSubmit={e => { e.preventDefault(); void registrar(); }}>
      <fieldset disabled={busy}><legend>Documento</legend>
        <label htmlFor="paciente-tipo">Tipo de documento</label>
        <select id="paciente-tipo" value={tipo} onChange={e => setTipo(e.target.value as TipoDocumento)}>
          {(Object.keys(formatos) as TipoDocumento[]).map(t => <option key={t} value={t}>{formatos[t].nombre}</option>)}
        </select>
        <label htmlFor="paciente-numero">Número de documento</label>
        <input id="paciente-numero" required autoComplete="off" maxLength={9} pattern={formatos[tipo].patron}
          inputMode={tipo === 'DNI' ? 'numeric' : 'text'} aria-describedby="paciente-numero-ayuda" {...campo('numeroDocumento')} />
        <p id="paciente-numero-ayuda" className="access-help">{formatos[tipo].ayuda} No se permite registrar dos pacientes con el mismo documento.</p>
      </fieldset>
      <fieldset disabled={busy}><legend>Datos personales</legend>
        <label htmlFor="paciente-nombres">Nombres</label>
        <input id="paciente-nombres" required maxLength={100} autoComplete="off" {...campo('nombres')} />
        <label htmlFor="paciente-apellidos">Apellidos</label>
        <input id="paciente-apellidos" required maxLength={100} autoComplete="off" {...campo('apellidos')} />
        <label htmlFor="paciente-nacimiento">Fecha de nacimiento</label>
        <input id="paciente-nacimiento" type="date" required min="1900-01-01" max={hoyLocal()} {...campo('fechaNacimiento')} />
      </fieldset>
      <fieldset disabled={busy}><legend>Contacto (opcional)</legend>
        <label htmlFor="paciente-telefono">Teléfono</label>
        <input id="paciente-telefono" type="tel" maxLength={20} autoComplete="off" {...campo('telefono')} />
        <label htmlFor="paciente-correo">Correo electrónico</label>
        <input id="paciente-correo" type="email" maxLength={254} autoComplete="off" {...campo('correo')} />
      </fieldset>
      {error && <p role="alert" className="error">{error}</p>}
      {registrado && <p role="status" className="notice">Paciente registrado con el código <strong>{registrado.codigo}</strong>.</p>}
      <button className="primary-button" disabled={busy}>{busy ? 'Registrando paciente…' : 'Registrar paciente'}</button>
    </form>
  </section>;
}
