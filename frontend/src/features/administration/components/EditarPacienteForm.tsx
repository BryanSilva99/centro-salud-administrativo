import { useRef, useState, type ChangeEvent } from 'react';
import { ApiError } from '../../../shared/api/http';
import { pacientesApi, type Paciente, type TipoDocumento } from '../api/pacientes';

const formatos: Record<TipoDocumento, { nombre: string; ayuda: string; patron: string }> = {
  DNI: { nombre: 'DNI', ayuda: '8 dígitos.', patron: '[0-9]{8}' },
  CE: { nombre: 'Carné de extranjería', ayuda: '9 letras o números.', patron: '[A-Za-z0-9]{9}' },
};

function hoyLocal(): string {
  const ahora = new Date();
  return `${ahora.getFullYear()}-${String(ahora.getMonth() + 1).padStart(2, '0')}-${String(ahora.getDate()).padStart(2, '0')}`;
}
export function errorEdicionPaciente(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.code === 'DOCUMENTO_DUPLICADO') return 'Ya existe otro paciente con ese documento.';
    if (error.code === 'CONFLICTO') return 'Otra persona cambió esta ficha. Recárgala antes de guardar.';
    if (error.code === 'NO_EXISTE') return 'El paciente ya no existe.';
    if (error.status === 403) return 'No tienes permiso para actualizar pacientes.';
  }
  return error instanceof Error ? error.message : 'No se pudo guardar el paciente.';
}

type Props = { paciente: Paciente; onSaved: (paciente: Paciente) => void; onReload: () => void; onExpired: () => void };

/** El código interno no se edita: lo conserva la actualización. */
export function EditarPacienteForm({ paciente, onSaved, onReload, onExpired }: Props) {
  const [tipo, setTipo] = useState<TipoDocumento>(paciente.tipoDocumento);
  const [datos, setDatos] = useState({
    numeroDocumento: paciente.numeroDocumento, nombres: paciente.nombres, apellidos: paciente.apellidos,
    fechaNacimiento: paciente.fechaNacimiento, telefono: paciente.telefono ?? '', correo: paciente.correo ?? '',
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [conflicto, setConflicto] = useState(false);
  const enviando = useRef(false);
  const campo = (nombre: keyof typeof datos) => ({
    value: datos[nombre],
    onChange: (e: ChangeEvent<HTMLInputElement>) => setDatos(actual => ({ ...actual, [nombre]: e.target.value })),
  });

  async function guardar() {
    if (enviando.current) return;
    enviando.current = true; setBusy(true); setError('');
    try {
      const actualizado = await pacientesApi.actualizar(paciente, {
        tipoDocumento: tipo,
        numeroDocumento: datos.numeroDocumento.trim(),
        nombres: datos.nombres.trim(),
        apellidos: datos.apellidos.trim(),
        fechaNacimiento: datos.fechaNacimiento,
        telefono: datos.telefono.trim() || null,
        correo: datos.correo.trim() || null,
      });
      onSaved(actualizado);
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) onExpired();
      else {
        if (e instanceof ApiError && e.code === 'CONFLICTO') setConflicto(true);
        setError(errorEdicionPaciente(e));
      }
    } finally { enviando.current = false; setBusy(false); }
  }

  return <form aria-label={`Editar ${paciente.codigo}`} onSubmit={e => { e.preventDefault(); void guardar(); }}>
    <h3>Editar paciente {paciente.codigo}</h3>
    <fieldset disabled={busy || conflicto}><legend>Documento</legend>
      <label htmlFor="editar-paciente-tipo">Tipo de documento</label>
      <select id="editar-paciente-tipo" value={tipo} onChange={e => setTipo(e.target.value as TipoDocumento)}>
        {(Object.keys(formatos) as TipoDocumento[]).map(t => <option key={t} value={t}>{formatos[t].nombre}</option>)}
      </select>
      <label htmlFor="editar-paciente-numero">Número de documento</label>
      <input id="editar-paciente-numero" required autoComplete="off" maxLength={9} pattern={formatos[tipo].patron}
        inputMode={tipo === 'DNI' ? 'numeric' : 'text'} aria-describedby="editar-paciente-numero-ayuda" {...campo('numeroDocumento')} />
      <p id="editar-paciente-numero-ayuda" className="access-help">{formatos[tipo].ayuda} Un cambio de documento vuelve a verificar que no esté en uso.</p>
    </fieldset>
    <fieldset disabled={busy || conflicto}><legend>Datos personales</legend>
      <label htmlFor="editar-paciente-nombres">Nombres</label>
      <input id="editar-paciente-nombres" required maxLength={100} autoComplete="off" {...campo('nombres')} />
      <label htmlFor="editar-paciente-apellidos">Apellidos</label>
      <input id="editar-paciente-apellidos" required maxLength={100} autoComplete="off" {...campo('apellidos')} />
      <label htmlFor="editar-paciente-nacimiento">Fecha de nacimiento</label>
      <input id="editar-paciente-nacimiento" type="date" required min="1900-01-01" max={hoyLocal()} {...campo('fechaNacimiento')} />
    </fieldset>
    <fieldset disabled={busy || conflicto}><legend>Contacto (opcional)</legend>
      <label htmlFor="editar-paciente-telefono">Teléfono</label>
      <input id="editar-paciente-telefono" type="tel" maxLength={20} autoComplete="off" {...campo('telefono')} />
      <label htmlFor="editar-paciente-correo">Correo electrónico</label>
      <input id="editar-paciente-correo" type="email" maxLength={254} autoComplete="off" {...campo('correo')} />
    </fieldset>
    {error && <p role="alert" className="error">{error}</p>}
    {conflicto && <button type="button" onClick={onReload}>Recargar paciente</button>}
    <button className="primary-button" disabled={busy || conflicto}>{busy ? 'Guardando paciente…' : 'Guardar paciente'}</button>
  </form>;
}
