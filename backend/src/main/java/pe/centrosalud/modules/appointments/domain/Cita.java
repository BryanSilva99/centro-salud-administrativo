package pe.centrosalud.modules.appointments.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/** Entidad de dominio de la reserva; no depende de Spring ni de JPA. */
public final class Cita {
  private final UUID id;
  private final UUID pacienteId;
  private final UUID profesionalId;
  private final UUID servicioId;
  private final UUID consultorioId;
  private final int duracionMinutos;
  private final String modalidad;
  private final BigDecimal importe;
  private final UUID creadaPor;
  private final Instant creadaEn;
  private EstadoCita estado;
  private Instant inicio;
  private Instant fin;
  private Instant llegadaEn;
  private UUID llegadaPor;
  private Instant atencionIniciadaEn;
  private UUID atencionIniciadaPor;
  private Instant atencionFinalizadaEn;
  private UUID atencionFinalizadaPor;
  private TipoCierreCita tipoCierre;
  private String motivoCierre;
  private Instant cerradaEn;
  private UUID cerradaPor;
  private long version;

  private Cita(
      UUID id,
      UUID pacienteId,
      UUID profesionalId,
      UUID servicioId,
      UUID consultorioId,
      HorarioCita horario,
      int duracionMinutos,
      String modalidad,
      BigDecimal importe,
      UUID creadaPor,
      Instant creadaEn,
      EstadoCita estado,
      long version,
      Instant llegadaEn,
      UUID llegadaPor,
      Instant atencionIniciadaEn,
      UUID atencionIniciadaPor,
      Instant atencionFinalizadaEn,
      UUID atencionFinalizadaPor,
      TipoCierreCita tipoCierre,
      String motivoCierre,
      Instant cerradaEn,
      UUID cerradaPor) {
    this.id = Objects.requireNonNull(id);
    this.pacienteId = Objects.requireNonNull(pacienteId);
    this.profesionalId = Objects.requireNonNull(profesionalId);
    this.servicioId = Objects.requireNonNull(servicioId);
    this.consultorioId = Objects.requireNonNull(consultorioId);
    this.inicio = Objects.requireNonNull(horario).inicio();
    this.fin = horario.fin();
    if (duracionMinutos <= 0) throw new IllegalArgumentException("La duracion debe ser positiva");
    this.duracionMinutos = duracionMinutos;
    this.modalidad = Objects.requireNonNull(modalidad);
    if (modalidad.isBlank()) throw new IllegalArgumentException("La modalidad es obligatoria");
    this.importe = Objects.requireNonNull(importe);
    if (importe.signum() < 0)
      throw new IllegalArgumentException("El importe no puede ser negativo");
    this.creadaPor = Objects.requireNonNull(creadaPor);
    this.creadaEn = Objects.requireNonNull(creadaEn);
    this.estado = Objects.requireNonNull(estado);
    this.version = version;
    this.llegadaEn = llegadaEn;
    this.llegadaPor = llegadaPor;
    this.atencionIniciadaEn = atencionIniciadaEn;
    this.atencionIniciadaPor = atencionIniciadaPor;
    this.atencionFinalizadaEn = atencionFinalizadaEn;
    this.atencionFinalizadaPor = atencionFinalizadaPor;
    this.tipoCierre = tipoCierre;
    this.motivoCierre = motivoCierre;
    this.cerradaEn = cerradaEn;
    this.cerradaPor = cerradaPor;
  }

  public static Cita programar(
      UUID id,
      UUID pacienteId,
      UUID profesionalId,
      UUID servicioId,
      UUID consultorioId,
      HorarioCita horario,
      int duracionMinutos,
      String modalidad,
      BigDecimal importe,
      UUID actorId,
      Clock clock) {
    Objects.requireNonNull(clock, "El reloj es obligatorio");
    return new Cita(
        id,
        pacienteId,
        profesionalId,
        servicioId,
        consultorioId,
        horario,
        duracionMinutos,
        modalidad,
        importe,
        actorId,
        clock.instant(),
        EstadoCita.PROGRAMADA,
        0,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  /** Compatibilidad para reconstrucciones que todavia no tienen marcas de atencion o cierre. */
  public static Cita restaurar(
      UUID id,
      UUID pacienteId,
      UUID profesionalId,
      UUID servicioId,
      UUID consultorioId,
      HorarioCita horario,
      int duracionMinutos,
      String modalidad,
      BigDecimal importe,
      UUID creadaPor,
      Instant creadaEn,
      EstadoCita estado,
      long version) {
    return restaurar(
        id,
        pacienteId,
        profesionalId,
        servicioId,
        consultorioId,
        horario,
        duracionMinutos,
        modalidad,
        importe,
        creadaPor,
        creadaEn,
        estado,
        version,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public static Cita restaurar(
      UUID id,
      UUID pacienteId,
      UUID profesionalId,
      UUID servicioId,
      UUID consultorioId,
      HorarioCita horario,
      int duracionMinutos,
      String modalidad,
      BigDecimal importe,
      UUID creadaPor,
      Instant creadaEn,
      EstadoCita estado,
      long version,
      Instant llegadaEn,
      UUID llegadaPor,
      Instant atencionIniciadaEn,
      UUID atencionIniciadaPor,
      Instant atencionFinalizadaEn,
      UUID atencionFinalizadaPor,
      TipoCierreCita tipoCierre,
      String motivoCierre,
      Instant cerradaEn,
      UUID cerradaPor) {
    return new Cita(
        id,
        pacienteId,
        profesionalId,
        servicioId,
        consultorioId,
        horario,
        duracionMinutos,
        modalidad,
        importe,
        creadaPor,
        creadaEn,
        estado,
        version,
        llegadaEn,
        llegadaPor,
        atencionIniciadaEn,
        atencionIniciadaPor,
        atencionFinalizadaEn,
        atencionFinalizadaPor,
        tipoCierre,
        motivoCierre,
        cerradaEn,
        cerradaPor);
  }

  public Reprogramacion reprogramar(
      HorarioCita nuevoHorario, String motivo, UUID actorId, long versionEsperada, Clock clock) {
    Objects.requireNonNull(nuevoHorario, "El nuevo horario es obligatorio");
    Objects.requireNonNull(actorId, "El actor es obligatorio");
    Objects.requireNonNull(clock, "El reloj es obligatorio");
    if (estado != EstadoCita.PROGRAMADA)
      throw new IllegalStateException("Solo una cita programada puede reprogramarse");
    if (version != versionEsperada)
      throw new IllegalStateException("La cita fue modificada por otra operacion");
    String motivoNormalizado = Objects.requireNonNull(motivo, "El motivo es obligatorio").trim();
    if (motivoNormalizado.isEmpty()) throw new IllegalArgumentException("El motivo es obligatorio");
    var historial =
        new Reprogramacion(
            id,
            actorId,
            inicio,
            fin,
            nuevoHorario.inicio(),
            nuevoHorario.fin(),
            motivoNormalizado,
            clock.instant());
    inicio = nuevoHorario.inicio();
    fin = nuevoHorario.fin();
    version++;
    return historial;
  }

  /** Registra la llegada una sola vez, respetando el dia local del centro. */
  public void registrarLlegada(UUID actorId, long versionEsperada, Clock clock, ZoneId zona) {
    Objects.requireNonNull(actorId, "El actor es obligatorio");
    Objects.requireNonNull(clock, "El reloj es obligatorio");
    Objects.requireNonNull(zona, "La zona horaria es obligatoria");
    if (llegadaEn != null) return;
    if (version != versionEsperada)
      throw new IllegalStateException("La cita fue modificada por otra operacion");
    if (estado != EstadoCita.PROGRAMADA)
      throw new IllegalStateException("Solo una cita programada puede registrar llegada");
    Instant momento = clock.instant();
    var fechaCita = inicio.atZone(zona).toLocalDate();
    var fechaActual = momento.atZone(zona).toLocalDate();
    if (!fechaCita.equals(fechaActual))
      throw new IllegalArgumentException(
          "La llegada solo puede registrarse en la fecha de la cita");
    llegadaEn = momento;
    llegadaPor = actorId;
    estado = EstadoCita.EN_ESPERA;
    version++;
  }

  /** Inicia la atencion una sola vez y conserva el primer momento registrado. */
  public void iniciarAtencion(UUID actorId, long versionEsperada, Clock clock) {
    Objects.requireNonNull(actorId, "El actor es obligatorio");
    Objects.requireNonNull(clock, "El reloj es obligatorio");
    if (atencionIniciadaEn != null) return;
    if (version != versionEsperada)
      throw new IllegalStateException("La cita fue modificada por otra operacion");
    if (estado != EstadoCita.EN_ESPERA)
      throw new IllegalStateException("Solo una cita en espera puede iniciar atencion");
    if (llegadaEn == null)
      throw new IllegalStateException("La llegada es obligatoria antes de iniciar atencion");
    Instant momento = clock.instant();
    if (momento.isBefore(llegadaEn))
      throw new IllegalArgumentException("El inicio no puede preceder a la llegada");
    atencionIniciadaEn = momento;
    atencionIniciadaPor = actorId;
    estado = EstadoCita.EN_ATENCION;
    version++;
  }

  /** Finaliza la atencion una sola vez y conserva el primer momento registrado. */
  public void finalizarAtencion(UUID actorId, long versionEsperada, Clock clock) {
    Objects.requireNonNull(actorId, "El actor es obligatorio");
    Objects.requireNonNull(clock, "El reloj es obligatorio");
    if (atencionFinalizadaEn != null) return;
    if (version != versionEsperada)
      throw new IllegalStateException("La cita fue modificada por otra operacion");
    if (estado != EstadoCita.EN_ATENCION)
      throw new IllegalStateException("Solo una atencion en curso puede finalizar");
    if (atencionIniciadaEn == null)
      throw new IllegalStateException("El inicio es obligatorio antes de finalizar");
    Instant momento = clock.instant();
    if (momento.isBefore(atencionIniciadaEn))
      throw new IllegalArgumentException("La finalizacion no puede preceder al inicio");
    atencionFinalizadaEn = momento;
    atencionFinalizadaPor = actorId;
    estado = EstadoCita.ATENDIDA;
    version++;
  }

  /** Cancela una cita programada conservando la razon y la trazabilidad local. */
  public void cancelar(UUID actorId, long versionEsperada, String motivo, Clock clock) {
    Objects.requireNonNull(actorId, "El actor es obligatorio");
    Objects.requireNonNull(clock, "El reloj es obligatorio");
    if (tipoCierre == TipoCierreCita.CANCELACION) return;
    if (version != versionEsperada)
      throw new IllegalStateException("La cita fue modificada por otra operacion");
    if (estado != EstadoCita.PROGRAMADA)
      throw new IllegalStateException("Solo una cita programada puede cancelarse");
    cerrar(
        EstadoCita.CANCELADA,
        TipoCierreCita.CANCELACION,
        motivoObligatorio(motivo),
        actorId,
        clock.instant());
  }

  /** Marca como ausente una cita que termino sin registrar llegada. */
  public void marcarAusente(UUID actorId, long versionEsperada, Clock clock) {
    Objects.requireNonNull(actorId, "El actor es obligatorio");
    Objects.requireNonNull(clock, "El reloj es obligatorio");
    if (tipoCierre == TipoCierreCita.AUSENCIA) return;
    if (version != versionEsperada)
      throw new IllegalStateException("La cita fue modificada por otra operacion");
    if (estado != EstadoCita.PROGRAMADA)
      throw new IllegalStateException("Solo una cita programada puede marcarse ausente");
    if (llegadaEn != null)
      throw new IllegalStateException("Una cita con llegada no puede marcarse ausente");
    Instant momento = clock.instant();
    if (momento.isBefore(fin))
      throw new IllegalArgumentException("El horario de la cita todavia no termino");
    cerrar(EstadoCita.AUSENTE, TipoCierreCita.AUSENCIA, null, actorId, momento);
  }

  /** Registra el retiro de un paciente que ya estaba esperando. */
  public void retirarDeEspera(UUID actorId, long versionEsperada, String motivo, Clock clock) {
    Objects.requireNonNull(actorId, "El actor es obligatorio");
    Objects.requireNonNull(clock, "El reloj es obligatorio");
    if (tipoCierre == TipoCierreCita.RETIRO_ESPERA) return;
    if (version != versionEsperada)
      throw new IllegalStateException("La cita fue modificada por otra operacion");
    if (estado != EstadoCita.EN_ESPERA)
      throw new IllegalStateException("Solo una cita en espera puede retirarse");
    if (llegadaEn == null)
      throw new IllegalStateException("La llegada es obligatoria antes del retiro");
    if (atencionIniciadaEn != null)
      throw new IllegalStateException("Una atencion iniciada no puede retirarse");
    cerrar(
        EstadoCita.CANCELADA,
        TipoCierreCita.RETIRO_ESPERA,
        motivoObligatorio(motivo),
        actorId,
        clock.instant());
  }

  private void cerrar(
      EstadoCita nuevoEstado, TipoCierreCita tipo, String motivo, UUID actorId, Instant momento) {
    estado = nuevoEstado;
    tipoCierre = tipo;
    motivoCierre = motivo;
    cerradaPor = actorId;
    cerradaEn = momento;
    version++;
  }

  private static String motivoObligatorio(String motivo) {
    var normalizado = Objects.requireNonNull(motivo, "El motivo es obligatorio").trim();
    if (normalizado.isEmpty()) throw new IllegalArgumentException("El motivo es obligatorio");
    if (normalizado.length() > 500)
      throw new IllegalArgumentException("El motivo no puede superar 500 caracteres");
    return normalizado;
  }

  public UUID id() {
    return id;
  }

  public UUID pacienteId() {
    return pacienteId;
  }

  public UUID profesionalId() {
    return profesionalId;
  }

  public UUID servicioId() {
    return servicioId;
  }

  public UUID consultorioId() {
    return consultorioId;
  }

  public HorarioCita horario() {
    return new HorarioCita(inicio, fin);
  }

  public int duracionMinutos() {
    return duracionMinutos;
  }

  public String modalidad() {
    return modalidad;
  }

  public BigDecimal importe() {
    return importe;
  }

  public UUID creadaPor() {
    return creadaPor;
  }

  public Instant creadaEn() {
    return creadaEn;
  }

  public EstadoCita estado() {
    return estado;
  }

  public Instant llegadaEn() {
    return llegadaEn;
  }

  public UUID llegadaPor() {
    return llegadaPor;
  }

  public Instant atencionIniciadaEn() {
    return atencionIniciadaEn;
  }

  public UUID atencionIniciadaPor() {
    return atencionIniciadaPor;
  }

  public Instant atencionFinalizadaEn() {
    return atencionFinalizadaEn;
  }

  public UUID atencionFinalizadaPor() {
    return atencionFinalizadaPor;
  }

  public TipoCierreCita tipoCierre() {
    return tipoCierre;
  }

  public String motivoCierre() {
    return motivoCierre;
  }

  public Instant cerradaEn() {
    return cerradaEn;
  }

  public UUID cerradaPor() {
    return cerradaPor;
  }

  public long version() {
    return version;
  }
}
