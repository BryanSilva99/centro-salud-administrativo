import { useState } from 'react';
import { billingApi } from '../api/billing';

type Seccion = 'cargos' | 'sesiones' | 'pagos' | 'saldos' | 'reversiones' | 'cierre' | 'reporte';

export function BillingView() {
  const [seccion, setSeccion] = useState<Seccion>('cargos');
  const [mensaje, setMensaje] = useState('');

  const notificar = (msg: string) => {
    setMensaje(msg);
    setTimeout(() => setMensaje(''), 5000);
  };

  return (
    <section className="billing-view">
      <h1>Caja y Movimientos Económicos</h1>
      <nav className="billing-nav" aria-label="Secciones de caja">
        {(['cargos', 'sesiones', 'pagos', 'saldos', 'reversiones', 'cierre', 'reporte'] as Seccion[]).map((s) => (
          <button
            key={s}
            className={seccion === s ? 'active' : ''}
            onClick={() => setSeccion(s)}
          >
            {s.charAt(0).toUpperCase() + s.slice(1)}
          </button>
        ))}
      </nav>

      {mensaje && <div role="status" className="billing-notification">{mensaje}</div>}

      <div className="billing-content">
        {seccion === 'cargos' && <SeccionCargos onNotify={notificar} />}
        {seccion === 'sesiones' && <SeccionSesiones onNotify={notificar} />}
        {seccion === 'pagos' && <SeccionPagos onNotify={notificar} />}
        {seccion === 'saldos' && <SeccionSaldos onNotify={notificar} />}
        {seccion === 'reversiones' && <SeccionReversiones onNotify={notificar} />}
        {seccion === 'cierre' && <SeccionCierre onNotify={notificar} />}
        {seccion === 'reporte' && <SeccionReporte onNotify={notificar} />}
      </div>
    </section>
  );
}

function SeccionCargos({ onNotify }: { onNotify: (msg: string) => void }) {
  const [citaId, setCitaId] = useState('');
  const [pacienteId, setPacienteId] = useState('');
  const [importe, setImporte] = useState('');

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await billingApi.registrarCargo({ citaId, pacienteId, importe });
      onNotify('Cargo registrado correctamente');
      setCitaId(''); setPacienteId(''); setImporte('');
    } catch (err: any) {
      onNotify(`Error: ${err.message}`);
    }
  };

  return (
    <form onSubmit={submit} className="billing-form">
      <h2>Registrar Cargo (RF-19)</h2>
      <label>
        ID de Cita
        <input value={citaId} onChange={(e) => setCitaId(e.target.value)} required />
      </label>
      <label>
        ID de Paciente
        <input value={pacienteId} onChange={(e) => setPacienteId(e.target.value)} required />
      </label>
      <label>
        Importe (PEN)
        <input type="number" step="0.01" min="0.01" value={importe} onChange={(e) => setImporte(e.target.value)} required />
      </label>
      <button type="submit">Registrar Cargo</button>
    </form>
  );
}

function SeccionSesiones({ onNotify }: { onNotify: (msg: string) => void }) {
  const [cajeroId, setCajeroId] = useState('');
  const [fondoInicial, setFondoInicial] = useState('');

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await billingApi.abrirSesion({ cajeroId, fondoInicial });
      onNotify('Sesión de caja abierta correctamente');
      setCajeroId(''); setFondoInicial('');
    } catch (err: any) {
      onNotify(`Error: ${err.message}`);
    }
  };

  return (
    <form onSubmit={submit} className="billing-form">
      <h2>Abrir Sesión de Caja (RF-20)</h2>
      <label>
        ID de Cajero
        <input value={cajeroId} onChange={(e) => setCajeroId(e.target.value)} required />
      </label>
      <label>
        Fondo Inicial (PEN)
        <input type="number" step="0.01" min="0" value={fondoInicial} onChange={(e) => setFondoInicial(e.target.value)} required />
      </label>
      <button type="submit">Abrir Sesión</button>
    </form>
  );
}

function SeccionPagos({ onNotify }: { onNotify: (msg: string) => void }) {
  const [cargoId, setCargoId] = useState('');
  const [sesionCajaId, setSesionCajaId] = useState('');
  const [importe, setImporte] = useState('');
  const [medio, setMedio] = useState('EFECTIVO');
  const [referencia, setReferencia] = useState('');

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await billingApi.registrarPago({ cargoId, sesionCajaId, importe, medio, referencia });
      onNotify('Pago registrado correctamente');
      setCargoId(''); setSesionCajaId(''); setImporte(''); setReferencia('');
    } catch (err: any) {
      onNotify(`Error: ${err.message}`);
    }
  };

  return (
    <form onSubmit={submit} className="billing-form">
      <h2>Registrar Pago (RF-21)</h2>
      <label>
        ID de Cargo
        <input value={cargoId} onChange={(e) => setCargoId(e.target.value)} required />
      </label>
      <label>
        ID de Sesión de Caja
        <input value={sesionCajaId} onChange={(e) => setSesionCajaId(e.target.value)} required />
      </label>
      <label>
        Importe (PEN)
        <input type="number" step="0.01" min="0.01" value={importe} onChange={(e) => setImporte(e.target.value)} required />
      </label>
      <label>
        Medio de Pago
        <select value={medio} onChange={(e) => setMedio(e.target.value)}>
          <option value="EFECTIVO">Efectivo</option>
          <option value="TARJETA">Tarjeta</option>
          <option value="TRANSFERENCIA">Transferencia</option>
          <option value="OTRO">Otro</option>
        </select>
      </label>
      <label>
        Referencia (opcional)
        <input value={referencia} onChange={(e) => setReferencia(e.target.value)} />
      </label>
      <button type="submit">Registrar Pago</button>
    </form>
  );
}

function SeccionSaldos({ onNotify }: { onNotify: (msg: string) => void }) {
  const [cargoId, setCargoId] = useState('');
  const [saldo, setSaldo] = useState<any>(null);

  const consultar = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const data = await billingApi.consultarSaldo(cargoId);
      setSaldo(data);
    } catch (err: any) {
      onNotify(`Error: ${err.message}`);
    }
  };

  return (
    <div className="billing-form">
      <h2>Consultar Saldo (RF-22)</h2>
      <form onSubmit={consultar}>
        <label>
          ID de Cargo
          <input value={cargoId} onChange={(e) => setCargoId(e.target.value)} required />
        </label>
        <button type="submit">Consultar</button>
      </form>
      {saldo && (
        <div className="billing-result">
          <h3>Resultado</h3>
          <p>Importe: S/ {saldo.importe}</p>
          <p>Total Pagado: S/ {saldo.totalPagado}</p>
          <p>Total Reversiones: S/ {saldo.totalReversiones}</p>
          <p><strong>Saldo Pendiente: S/ {saldo.saldoPendiente}</strong></p>
        </div>
      )}
    </div>
  );
}

function SeccionReversiones({ onNotify }: { onNotify: (msg: string) => void }) {
  const [operacionId, setOperacionId] = useState('');
  const [tipo, setTipo] = useState<'CARGO' | 'PAGO'>('CARGO');
  const [solicitanteId, setSolicitanteId] = useState('');
  const [motivo, setMotivo] = useState('');

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await billingApi.solicitarReversion({ operacionId, tipo, solicitanteId, motivo });
      onNotify('Solicitud de reversión registrada');
      setOperacionId(''); setSolicitanteId(''); setMotivo('');
    } catch (err: any) {
      onNotify(`Error: ${err.message}`);
    }
  };

  return (
    <form onSubmit={submit} className="billing-form">
      <h2>Solicitar Reversión (RF-23)</h2>
      <label>
        ID de Operación
        <input value={operacionId} onChange={(e) => setOperacionId(e.target.value)} required />
      </label>
      <label>
        Tipo de Operación
        <select value={tipo} onChange={(e) => setTipo(e.target.value as 'CARGO' | 'PAGO')}>
          <option value="CARGO">Cargo</option>
          <option value="PAGO">Pago</option>
        </select>
      </label>
      <label>
        ID de Solicitante
        <input value={solicitanteId} onChange={(e) => setSolicitanteId(e.target.value)} required />
      </label>
      <label>
        Motivo
        <textarea value={motivo} onChange={(e) => setMotivo(e.target.value)} required />
      </label>
      <button type="submit">Solicitar Reversión</button>
    </form>
  );
}

function SeccionCierre({ onNotify }: { onNotify: (msg: string) => void }) {
  const [sesionId, setSesionId] = useState('');
  const [efectivoDeclarado, setEfectivoDeclarado] = useState('');
  const [motivoDiferencia, setMotivoDiferencia] = useState('');
  const [cierre, setCierre] = useState<any>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const data = await billingApi.cerrarCaja(sesionId, { efectivoDeclarado, motivoDiferencia });
      setCierre(data);
      onNotify('Caja cerrada correctamente');
    } catch (err: any) {
      onNotify(`Error: ${err.message}`);
    }
  };

  return (
    <div className="billing-form">
      <h2>Cerrar Caja (RF-24)</h2>
      <form onSubmit={submit}>
        <label>
          ID de Sesión
          <input value={sesionId} onChange={(e) => setSesionId(e.target.value)} required />
        </label>
        <label>
          Efectivo Declarado (PEN)
          <input type="number" step="0.01" min="0" value={efectivoDeclarado} onChange={(e) => setEfectivoDeclarado(e.target.value)} required />
        </label>
        <label>
          Motivo de Diferencia (si aplica)
          <input value={motivoDiferencia} onChange={(e) => setMotivoDiferencia(e.target.value)} />
        </label>
        <button type="submit">Cerrar Caja</button>
      </form>
      {cierre && (
        <div className="billing-result">
          <h3>Resultado del Cierre</h3>
          <p>Efectivo Esperado: S/ {cierre.efectivoEsperado}</p>
          <p>Efectivo Declarado: S/ {cierre.efectivoDeclarado}</p>
          <p><strong>Diferencia: S/ {cierre.diferencia}</strong></p>
        </div>
      )}
    </div>
  );
}

function SeccionReporte({ onNotify }: { onNotify: (msg: string) => void }) {
  const [fechaInicio, setFechaInicio] = useState('');
  const [fechaFin, setFechaFin] = useState('');
  const [reporte, setReporte] = useState<any>(null);

  const consultar = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const data = await billingApi.reporteEconomico(fechaInicio, fechaFin);
      setReporte(data);
    } catch (err: any) {
      onNotify(`Error: ${err.message}`);
    }
  };

  return (
    <div className="billing-form">
      <h2>Reporte Económico (RF-27)</h2>
      <form onSubmit={consultar}>
        <label>
          Fecha Inicio
          <input type="date" value={fechaInicio} onChange={(e) => setFechaInicio(e.target.value)} required />
        </label>
        <label>
          Fecha Fin
          <input type="date" value={fechaFin} onChange={(e) => setFechaFin(e.target.value)} required />
        </label>
        <button type="submit">Generar Reporte</button>
      </form>
      {reporte && (
        <div className="billing-result">
          <h3>Reporte Económico</h3>
          <p>Período: {reporte.fechaInicio} al {reporte.fechaFin}</p>
          <p>Total Cobros: S/ {reporte.totalCobros}</p>
          <p>Total Devoluciones: S/ {reporte.totalDevoluciones}</p>
          <p><strong>Ingresos Netos: S/ {reporte.ingresosNetos}</strong></p>
        </div>
      )}
    </div>
  );
}
