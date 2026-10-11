import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { ApiError } from '../../../shared/api/http';
import { type Cuenta } from '../../identity/api/auth';
import { appointments, type Atencion, type Cita, type Contexto, type Dashboard, type Horario } from '../api/appointments';

const CENTRO = 'America/Lima';
const localDate = () => {
  const parts = new Intl.DateTimeFormat('en', { timeZone: CENTRO, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date());
  const value = (type: string) => parts.find(part => part.type === type)?.value || '';
  return `${value('year')}-${value('month')}-${value('day')}`;
};
const localDateOf = (value: string) => {
  const parts = new Intl.DateTimeFormat('en', { timeZone: CENTRO, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date(value));
  const part = (type: string) => parts.find(item => item.type === type)?.value || '';
  return `${part('year')}-${part('month')}-${part('day')}`;
};
const time = (value: string) => new Intl.DateTimeFormat('es-PE', { timeZone: CENTRO, hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(value));
const dateTime = (value: string | null | undefined) => value ? `${localDateOf(value)} ${time(value)}` : '—';
const money = (value: number) => `S/ ${value.toFixed(2)}`;
const statusLabel: Record<string, string> = { PROGRAMADA: 'Programada', EN_ESPERA: 'En espera', EN_ATENCION: 'En atención', ATENDIDA: 'Atendida', CANCELADA: 'Cancelada', AUSENTE: 'Ausente' };
const statusKeys = ['PROGRAMADA', 'EN_ESPERA', 'EN_ATENCION', 'ATENDIDA', 'CANCELADA', 'AUSENTE'];

type Booking = { pacienteId: string; profesionalId: string; servicioId: string; consultorioId: string; modalidad: string; fecha: string; hora: string };
type View = 'agenda' | 'atencion' | 'dashboard';
type ClosureAction = 'cancelar' | 'ausencia' | 'retiro';

export function AppointmentsWorkspace({ cuenta, onExit }: { cuenta: Cuenta; onExit: (message: string) => void }) {
  const [context, setContext] = useState<Contexto | null>(null);
  const [view, setView] = useState<View>('agenda');
  const [date, setDate] = useState(localDate);
  const [filterProfessional, setFilterProfessional] = useState('');
  const [filterService, setFilterService] = useState('');
  const [filterRoom, setFilterRoom] = useState('');
  const [agenda, setAgenda] = useState<Cita[]>([]);
  const [attention, setAttention] = useState<Atencion | null>(null);
  const [dashboard, setDashboard] = useState<Dashboard | null>(null);
  const [slots, setSlots] = useState<Horario[]>([]);
  const [booking, setBooking] = useState<Booking>({ pacienteId: '', profesionalId: '', servicioId: '', consultorioId: '', modalidad: '', fecha: localDate(), hora: '' });
  const [reprogramming, setReprogramming] = useState<Cita | null>(null);
  const [closing, setClosing] = useState<{ cita: Cita; action: ClosureAction } | null>(null);
  const [reprogramDate, setReprogramDate] = useState('');
  const [reprogramTime, setReprogramTime] = useState('');
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingAgenda, setLoadingAgenda] = useState(false);
  const [loadingAttention, setLoadingAttention] = useState(false);
  const [loadingDashboard, setLoadingDashboard] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [reload, setReload] = useState(0);
  const [now, setNow] = useState(() => Date.now());
  const title = useRef<HTMLHeadingElement>(null);
  const dialogTitle = useRef<HTMLHeadingElement>(null);
  const dialogReturnFocus = useRef<HTMLElement | null>(null);
  const expired = useCallback(() => onExit('Tu sesión terminó. Vuelve a ingresar para continuar.'), [onExit]);

  const can = useCallback((operation: string) => context?.capacidades ? context.capacidades.includes(operation) : false, [context]);

  useEffect(() => {
    title.current?.focus();
    const controller = new AbortController();
    appointments.contexto(controller.signal).then(value => {
      if (!Array.isArray(value?.pacientes) || !Array.isArray(value?.servicios)) throw new Error('El catálogo no tiene el formato esperado.');
      if (!controller.signal.aborted) setContext(value);
    }).catch(failure => {
      if (!controller.signal.aborted) {
        if (failure instanceof ApiError && failure.status === 401) expired();
        else setError(failure instanceof ApiError && failure.status === 403
          ? 'La cuenta no tiene permisos para consultar citas.'
          : 'No pudimos cargar el catálogo de citas. Revisa la configuración local e inténtalo de nuevo.');
      }
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [expired]);

  useEffect(() => {
    if (!context) return;
    setBooking(current => ({
      ...current,
      pacienteId: current.pacienteId || context.pacientes[0]?.id || '',
      profesionalId: current.profesionalId || context.profesionales[0]?.id || '',
      servicioId: current.servicioId || context.servicios[0]?.id || '',
      consultorioId: current.consultorioId || context.consultorios[0]?.id || '',
      modalidad: current.modalidad || context.servicios[0]?.modalidades[0]?.codigo || '',
    }));
  }, [context]);

  useEffect(() => {
    if (!context) return;
    const controller = new AbortController();
    setLoadingAgenda(true);
    appointments.agenda({ fecha: date, profesionalId: filterProfessional || undefined, servicioId: filterService || undefined, consultorioId: filterRoom || undefined }, controller.signal)
      .then(value => { if (!controller.signal.aborted) setAgenda(Array.isArray(value) ? value : []); })
      .catch(failure => { if (!controller.signal.aborted) handleFailure(failure, expired, setError, 'No pudimos actualizar la agenda.'); })
      .finally(() => { if (!controller.signal.aborted) setLoadingAgenda(false); });
    return () => controller.abort();
  }, [context, date, filterProfessional, filterService, filterRoom, reload, expired]);

  useEffect(() => {
    if (!context || view !== 'atencion') return;
    const controller = new AbortController();
    setLoadingAttention(true);
    appointments.atencion({ fecha: date, profesionalId: filterProfessional || undefined }, controller.signal)
      .then(value => { if (!controller.signal.aborted) setAttention(value); })
      .catch(failure => { if (!controller.signal.aborted) handleFailure(failure, expired, setError, 'No pudimos cargar la sala de espera.'); })
      .finally(() => { if (!controller.signal.aborted) setLoadingAttention(false); });
    return () => controller.abort();
  }, [context, view, date, filterProfessional, reload, expired]);

  useEffect(() => {
    if (!context || view !== 'dashboard') return;
    const controller = new AbortController();
    setLoadingDashboard(true);
    appointments.dashboard({ fecha: date, profesionalId: filterProfessional || undefined, servicioId: filterService || undefined, consultorioId: filterRoom || undefined }, controller.signal)
      .then(value => { if (!controller.signal.aborted) setDashboard(value); })
      .catch(failure => { if (!controller.signal.aborted) handleFailure(failure, expired, setError, 'No pudimos cargar el dashboard.'); })
      .finally(() => { if (!controller.signal.aborted) setLoadingDashboard(false); });
    return () => controller.abort();
  }, [context, view, date, filterProfessional, filterService, filterRoom, reload, expired]);

  useEffect(() => {
    if (!context || !can('PROGRAMAR') || !booking.pacienteId || !booking.profesionalId || !booking.servicioId || !booking.consultorioId || !booking.fecha) return;
    const controller = new AbortController();
    appointments.disponibilidad({ fecha: booking.fecha, pacienteId: booking.pacienteId, profesionalId: booking.profesionalId, servicioId: booking.servicioId, consultorioId: booking.consultorioId }, controller.signal)
      .then(value => { if (!controller.signal.aborted) setSlots(Array.isArray(value) ? value : []); })
      .catch(() => { if (!controller.signal.aborted) setSlots([]); });
    return () => controller.abort();
  }, [context, booking.pacienteId, booking.profesionalId, booking.servicioId, booking.consultorioId, booking.fecha, reload]);

  useEffect(() => { if (reprogramming || closing) dialogTitle.current?.focus(); }, [reprogramming, closing]);
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 15_000);
    return () => window.clearInterval(timer);
  }, []);

  const selectedService = useMemo(() => context?.servicios.find(service => service.id === booking.servicioId), [context, booking.servicioId]);
  const updateBooking = (key: keyof Booking, value: string) => setBooking(current => ({ ...current, [key]: value, ...(key === 'servicioId' ? { modalidad: context?.servicios.find(service => service.id === value)?.modalidades[0]?.codigo || '' } : {}) }));
  const refresh = () => setReload(value => value + 1);



  async function submitBooking(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!booking.hora) { setError('Selecciona un horario disponible.'); return; }
    setBusy(true); setError(''); setNotice('');
    try {
      await appointments.programar(booking);
      setNotice('La cita se programó correctamente.');
      setBooking(current => ({ ...current, hora: '' }));
      refresh();
    } catch (failure) { handleFailure(failure, expired, setError, 'No pudimos programar la cita.'); }
    finally { setBusy(false); }
  }

  function openReprogram(cita: Cita) {
    dialogReturnFocus.current = document.activeElement as HTMLElement;
    setReprogramming(cita); setReprogramDate(localDateOf(cita.inicio)); setReprogramTime(''); setReason(''); setError(''); setNotice('');
  }

  function closeReprogram() {
    setReprogramming(null);
    window.requestAnimationFrame(() => dialogReturnFocus.current?.focus());
  }

  function openClosure(cita: Cita, action: ClosureAction) {
    dialogReturnFocus.current = document.activeElement as HTMLElement;
    setClosing({ cita, action }); setReason(''); setError(''); setNotice('');
  }

  function closeClosure() {
    setClosing(null);
    window.requestAnimationFrame(() => dialogReturnFocus.current?.focus());
  }

  async function submitReprogram(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!reprogramming || !reprogramDate || !reprogramTime || !reason.trim()) { setError('Completa la nueva fecha, hora y motivo.'); return; }
    setBusy(true); setError(''); setNotice('');
    try {
      await appointments.reprogramar(reprogramming.id, { fecha: reprogramDate, hora: reprogramTime, motivo: reason.trim(), version: reprogramming.version });
      setNotice('La cita se reprogramó correctamente.'); closeReprogram(); refresh();
    } catch (failure) { handleFailure(failure, expired, setError, 'No pudimos reprogramar la cita.'); }
    finally { setBusy(false); }
  }

  async function submitClosure(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!closing) return;
    if (closing.action !== 'ausencia' && !reason.trim()) {
      setError('El motivo es obligatorio.');
      return;
    }
    setBusy(true); setError(''); setNotice('');
    try {
      let economicPending = false;
      if (closing.action === 'cancelar') {
        const result = await appointments.cancelar(closing.cita.id, { motivo: reason.trim(), version: closing.cita.version });
        economicPending = result.gestionEconomicaPendiente;
      } else if (closing.action === 'retiro') {
        const result = await appointments.retirarEspera(closing.cita.id, { motivo: reason.trim(), version: closing.cita.version });
        economicPending = result.gestionEconomicaPendiente;
      } else {
        await appointments.ausencia(closing.cita.id, closing.cita.version);
      }
      const success = closing.action === 'cancelar'
        ? 'La cita se canceló correctamente.'
        : closing.action === 'retiro'
          ? 'El retiro de la espera se registró correctamente.'
          : 'La ausencia se registró correctamente.';
      setNotice(economicPending ? `${success} Queda una gestión económica pendiente.` : success);
      closeClosure();
      refresh();
    } catch (failure) {
      handleFailure(failure, expired, setError, 'No pudimos cerrar la cita.');
    } finally { setBusy(false); }
  }

  async function transition(cita: Cita, action: 'llegada' | 'iniciarAtencion' | 'finalizarAtencion', confirmation: string, success: string) {
    if (!window.confirm(confirmation)) return;
    setBusy(true); setError(''); setNotice('');
    try {
      await appointments[action](cita.id, cita.version);
      setNotice(success);
      refresh();
    } catch (failure) { handleFailure(failure, expired, setError, 'No pudimos confirmar la operación.'); }
    finally { setBusy(false); }
  }

  const renderFilters = () => <div className="filter-row">
    <label>Profesional<select value={filterProfessional} onChange={event => setFilterProfessional(event.target.value)}><option value="">Todos</option>{context?.profesionales.map(item => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label>
    <label>Servicio<select value={filterService} onChange={event => setFilterService(event.target.value)}><option value="">Todos</option>{context?.servicios.map(item => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label>
    <label>Consultorio<select value={filterRoom} onChange={event => setFilterRoom(event.target.value)}><option value="">Todos</option>{context?.consultorios.map(item => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label>
  </div>;
  const renderAttentionFilters = () => <div className="filter-row">
    <label>Profesional<select value={filterProfessional} onChange={event => setFilterProfessional(event.target.value)}><option value="">Todos</option>{context?.profesionales.map(item => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label>
  </div>;

  const renderAppointment = (cita: Cita, actions = true) => {
    const isToday = date === localDate() && localDateOf(cita.inicio) === date;
    return <article className="appointment-card" key={cita.id}>
      <div className="appointment-time"><strong>{time(cita.inicio)}</strong><span>{time(cita.fin)}</span></div>
      <div className="appointment-info"><div className="appointment-topline"><strong>{cita.paciente}</strong><span className={`state-badge state-${cita.estado.toLowerCase()}`}>{statusLabel[cita.estado] || cita.estado}</span></div><p>{cita.servicio} · {cita.profesional}</p><small>{cita.consultorio} · {cita.modalidadNombre} · {money(cita.importe)}</small>{cita.llegadaEn && <small className="event-time">Llegada: {dateTime(cita.llegadaEn)}</small>}{cita.tipoCierre && <small className="event-time">{closureLabel(cita.tipoCierre)}{cita.cerradaEn ? ` · ${dateTime(cita.cerradaEn)}` : ''}{cita.motivoCierre ? ` · ${cita.motivoCierre}` : ''}</small>}</div>
      {actions && <div className="appointment-actions">
        {cita.estado === 'PROGRAMADA' && can('REPROGRAMAR') && <button type="button" className="link-button" disabled={busy} onClick={() => openReprogram(cita)}>Reprogramar</button>}
        {cita.estado === 'PROGRAMADA' && isToday && can('REGISTRAR_LLEGADA') && <button type="button" className="link-button" disabled={busy} onClick={() => transition(cita, 'llegada', `¿Registrar la llegada de ${cita.paciente}?`, 'La llegada se registró correctamente.')}>Registrar llegada</button>}
        {cita.estado === 'PROGRAMADA' && can('CANCELAR') && <button type="button" className="link-button" disabled={busy} onClick={() => openClosure(cita, 'cancelar')}>Cancelar cita</button>}
        {cita.estado === 'PROGRAMADA' && hasEnded(cita, now) && can('REGISTRAR_AUSENCIA') && <button type="button" className="link-button" disabled={busy} onClick={() => openClosure(cita, 'ausencia')}>Registrar ausencia</button>}
      </div>}
    </article>;
  };

  return <section className="appointments-workspace" aria-labelledby="citas-title">
    <div className="workspace-header"><div><p className="eyebrow">Agenda operativa</p><h1 id="citas-title" tabIndex={-1} ref={title}>Hola, {cuenta.identificador}</h1><p className="intro">Programa, recibe y organiza las citas del centro de salud.</p></div></div>
    {notice && <p className="notice" role="status">{notice}</p>}
    {error && <p className="error" role="alert" tabIndex={-1}>{error}</p>}
    <nav className="workspace-tabs" role="tablist" aria-label="Secciones de citas">
      <button role="tab" type="button" className={view === 'agenda' ? 'active' : ''} aria-selected={view === 'agenda'} onClick={() => setView('agenda')}>Agenda</button>
      <button role="tab" type="button" className={view === 'atencion' ? 'active' : ''} aria-selected={view === 'atencion'} onClick={() => setView('atencion')}>Atención</button>
      <button role="tab" type="button" className={view === 'dashboard' ? 'active' : ''} aria-selected={view === 'dashboard'} onClick={() => setView('dashboard')}>Dashboard</button>
    </nav>

    {view === 'agenda' && <div className="workspace-grid">
      <section className="agenda-panel" aria-labelledby="agenda-title"><div className="section-heading"><div><h2 id="agenda-title">Agenda del día</h2></div><label className="compact-field">Fecha<input type="date" value={date} onChange={event => setDate(event.target.value)} /></label></div>{renderFilters()}
        {loading || loadingAgenda ? <p className="empty-state" role="status">Cargando agenda…</p> : agenda.length === 0 ? <p className="empty-state">No hay citas para los filtros seleccionados.</p> : <div className="appointment-list">{agenda.map(cita => renderAppointment(cita))}</div>}
      </section>
      {can('PROGRAMAR') && <section className="booking-panel" aria-labelledby="booking-title"><h2 id="booking-title">Nueva cita</h2><p className="section-copy">Elige los recursos y confirma un horario libre.</p>
        {loading ? <p role="status">Cargando catálogo…</p> : !context ? <p className="empty-state">El catálogo de citas no está disponible en este entorno.</p> : <form onSubmit={submitBooking} aria-busy={busy}><label>Paciente<select required value={booking.pacienteId} onChange={event => updateBooking('pacienteId', event.target.value)}>{context.pacientes.map(item => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label><label>Servicio<select required value={booking.servicioId} onChange={event => updateBooking('servicioId', event.target.value)}>{context.servicios.map(item => <option key={item.id} value={item.id}>{item.nombre} ({item.duracionMinutos} min)</option>)}</select></label><div className="two-fields"><label>Profesional<select required value={booking.profesionalId} onChange={event => updateBooking('profesionalId', event.target.value)}>{context.profesionales.map(item => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label><label>Consultorio<select required value={booking.consultorioId} onChange={event => updateBooking('consultorioId', event.target.value)}>{context.consultorios.map(item => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select></label></div><label>Modalidad<select required value={booking.modalidad} onChange={event => updateBooking('modalidad', event.target.value)}>{selectedService?.modalidades.map(item => <option key={item.codigo} value={item.codigo}>{item.nombre} · {money(item.importe)}</option>)}</select></label><label>Fecha<input required type="date" value={booking.fecha} onChange={event => { updateBooking('fecha', event.target.value); setBooking(current => ({ ...current, hora: '' })); }} /></label><fieldset className="slot-field"><legend>Horarios disponibles</legend>{slots.length === 0 ? <p className="field-help">No hay horarios libres para esta selección.</p> : <div className="slot-grid">{slots.map(slot => <label className="slot-option" key={slot.inicio}><input type="radio" name="booking-slot" value={time(slot.inicio)} checked={booking.hora === time(slot.inicio)} onChange={event => updateBooking('hora', event.target.value)} /><span>{time(slot.inicio)}</span></label>)}</div>}</fieldset><button className="primary-button" type="submit" disabled={busy}>{busy ? 'Guardando…' : 'Programar cita'}<span aria-hidden="true">→</span></button></form>}
      </section>}
    </div>}

    {view === 'atencion' && <section className="operation-panel" aria-labelledby="attention-title"><div className="section-heading"><div><h2 id="attention-title">Sala de espera y atención</h2><p className="section-copy">Registra llegadas y sigue las atenciones activas.</p></div><label className="compact-field">Fecha<input type="date" value={date} onChange={event => setDate(event.target.value)} /></label></div>{renderAttentionFilters()}
      {loadingAttention ? <p className="empty-state" role="status">Cargando sala de espera…</p> : <div className="attention-columns"><section aria-labelledby="waiting-title"><h3 id="waiting-title">En espera <span className="count-pill">{attention?.enEspera.length || 0}</span></h3>{!attention?.enEspera.length ? <p className="empty-state">No hay pacientes en espera.</p> : <div className="appointment-list">{attention.enEspera.map(cita => <article className="appointment-card" key={cita.id}><div className="appointment-time"><strong>{time(cita.inicio)}</strong><span>{wait(cita.llegadaEn, now)}</span></div><div className="appointment-info"><div className="appointment-topline"><strong>{cita.paciente}</strong><span className="state-badge state-en_espera">En espera</span></div><p>{cita.profesional} · {cita.servicio}</p><small>Llegó {dateTime(cita.llegadaEn)}</small></div><div className="appointment-actions">{can('INICIAR_ATENCION') && (!context?.profesionalAsignadoId || context.profesionalAsignadoId === cita.profesionalId) && <button type="button" className="link-button" disabled={busy} onClick={() => transition(cita, 'iniciarAtencion', `¿Iniciar la atención de ${cita.paciente}?`, 'La atención inició correctamente.')}>Iniciar atención</button>}{can('REGISTRAR_RETIRO') && <button type="button" className="link-button" disabled={busy} onClick={() => openClosure(cita, 'retiro')}>Registrar retiro</button>}</div></article>)}</div>}</section><section aria-labelledby="active-title"><h3 id="active-title">En atención <span className="count-pill">{attention?.enAtencion.length || 0}</span></h3>{!attention?.enAtencion.length ? <p className="empty-state">No hay atenciones en curso.</p> : <div className="appointment-list">{attention.enAtencion.map(cita => <article className="appointment-card" key={cita.id}><div className="appointment-time"><strong>{time(cita.inicio)}</strong><span>En curso</span></div><div className="appointment-info"><div className="appointment-topline"><strong>{cita.paciente}</strong><span className="state-badge state-en_atencion">En atención</span></div><p>{cita.profesional} · {cita.servicio}</p><small>Inició {dateTime(cita.atencionIniciadaEn)}</small></div>{can('FINALIZAR_ATENCION') && (!context?.profesionalAsignadoId || context.profesionalAsignadoId === cita.profesionalId) && <button type="button" className="link-button" disabled={busy} onClick={() => transition(cita, 'finalizarAtencion', `¿Finalizar la atención de ${cita.paciente}?`, 'La atención finalizó correctamente.')}>Finalizar atención</button>}</article>)}</div>}</section></div>}
    </section>}

    {view === 'dashboard' && <section className="dashboard-panel" aria-labelledby="dashboard-title"><div className="section-heading"><div><h2 id="dashboard-title">Dashboard operativo</h2><p className="section-copy">Resumen de las citas del día según sus estados confirmados.</p></div><label className="compact-field">Fecha<input type="date" value={date} onChange={event => setDate(event.target.value)} /></label></div>{renderFilters()}{loadingDashboard ? <p className="empty-state" role="status">Cargando dashboard…</p> : !dashboard ? <p className="empty-state">No hay datos para mostrar.</p> : <><div className="dashboard-cards"><article><span>Total de citas</span><strong>{dashboard.total}</strong></article><article><span>En espera</span><strong>{dashboard.enEspera}</strong></article><article><span>Atendidas</span><strong>{dashboard.atendidas}</strong></article><article><span>Ausentes</span><strong>{dashboard.ausentes}</strong></article></div><div className="dashboard-states">{statusKeys.map(status => <div key={status}><span className={`state-badge state-${status.toLowerCase()}`}>{statusLabel[status]}</span><strong>{dashboard.porEstado[status] || 0}</strong></div>)}</div><p className="updated-at">Actualizado {dateTime(dashboard.actualizadoEn)} · filtros aplicados para {dashboard.fecha}</p><button type="button" className="secondary-button" onClick={refresh}>Actualizar dashboard</button></>}</section>}

    {reprogramming && <div className="dialog-backdrop"><section className="dialog" role="dialog" aria-modal="true" aria-labelledby="reprogram-title"><div className="section-heading"><div><h2 id="reprogram-title" tabIndex={-1} ref={dialogTitle}>Reprogramar cita</h2></div><button type="button" className="dialog-close" aria-label="Cerrar" onClick={closeReprogram}>×</button></div><p className="section-copy">{reprogramming.paciente} · {reprogramming.servicio}. La duración y el importe se conservarán.</p><form onSubmit={submitReprogram}><label>Nueva fecha<input required type="date" value={reprogramDate} onChange={event => setReprogramDate(event.target.value)} /></label><label>Nueva hora<input required type="time" step="900" value={reprogramTime} onChange={event => setReprogramTime(event.target.value)} /></label><label>Motivo<textarea required minLength={3} maxLength={500} value={reason} onChange={event => setReason(event.target.value)} /></label><div className="dialog-actions"><button type="button" className="secondary-button" onClick={closeReprogram}>Cancelar</button><button type="submit" className="primary-button" disabled={busy}>{busy ? 'Guardando…' : 'Confirmar cambio'}</button></div></form></section></div>}
    {closing && <div className="dialog-backdrop"><section className="dialog" role="dialog" aria-modal="true" aria-labelledby="closure-title"><div className="section-heading"><div><p className="eyebrow">Cierre de cita</p><h2 id="closure-title" tabIndex={-1} ref={dialogTitle}>{closing.action === 'cancelar' ? 'Cancelar cita' : closing.action === 'retiro' ? 'Registrar retiro' : 'Registrar ausencia'}</h2></div><button type="button" className="dialog-close" aria-label="Cerrar" onClick={closeClosure}>×</button></div><p className="section-copy">{closing.cita.paciente} · {closing.cita.servicio}.</p><form onSubmit={submitClosure}>{closing.action !== 'ausencia' && <label>Motivo<textarea required maxLength={500} value={reason} onChange={event => setReason(event.target.value)} /></label>}{closing.action === 'ausencia' && <p className="section-copy">Confirma que terminó el intervalo programado y no se registró una llegada.</p>}<div className="dialog-actions"><button type="button" className="secondary-button" onClick={closeClosure}>Cancelar</button><button type="submit" className="primary-button" disabled={busy}>{busy ? 'Guardando…' : closing.action === 'ausencia' ? 'Confirmar ausencia' : 'Confirmar cierre'}</button></div></form></section></div>}
  </section>;
}

function wait(arrival: string | null | undefined, now: number) {
  if (!arrival) return '—';
  const seconds = Math.max(0, Math.floor((now - new Date(arrival).getTime()) / 1000));
  const minutes = Math.floor(seconds / 60);
  return minutes < 60 ? `${minutes} min de espera` : `${Math.floor(minutes / 60)} h ${minutes % 60} min de espera`;
}

function hasEnded(cita: Cita, now: number) {
  return now >= new Date(cita.fin).getTime();
}

function closureLabel(value: string) {
  return value === 'CANCELACION' ? 'Cancelación' : value === 'RETIRO_ESPERA' ? 'Retiro de espera' : 'Ausencia';
}

function handleFailure(failure: unknown, expired: () => void, setError: (value: string) => void, fallback: string) {
  if (failure instanceof ApiError && failure.status === 401) { expired(); return; }
  setError(messageFor(failure, fallback));
}

function messageFor(failure: unknown, fallback: string) {
  if (failure instanceof ApiError) {
    if (failure.status === 409) return 'La cita cambió o el recurso ya está ocupado. Actualiza la vista e inténtalo de nuevo.';
    if (failure.status === 422) return 'La operación no es válida para el estado o momento actual de la cita.';
    if (failure.status === 403) return 'La cuenta no tiene permisos para esta operación.';
    if (failure.status === 404) return 'La cita ya no existe o no está disponible.';
    if (failure.status === 401) return 'Tu sesión terminó. Vuelve a ingresar.';
  }
  return failure instanceof Error ? failure.message : fallback;
}
