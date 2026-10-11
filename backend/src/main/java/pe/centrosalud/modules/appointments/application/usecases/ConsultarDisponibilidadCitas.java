package pe.centrosalud.modules.appointments.application.usecases;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.CatalogoCitas;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.ports.DisponibilidadCitas;
import pe.centrosalud.modules.appointments.application.ports.OperacionCita;
import pe.centrosalud.modules.appointments.domain.HorarioCita;

public final class ConsultarDisponibilidadCitas {
  public static final ZoneId ZONA_CENTRO = ZoneId.of("America/Lima");
  private final CatalogoCitas catalogo;
  private final DisponibilidadCitas disponibilidad;
  private final Citas citas;
  private final AutorizacionCitas autorizacion;
  private final Clock clock;

  public ConsultarDisponibilidadCitas(
      CatalogoCitas catalogo,
      DisponibilidadCitas disponibilidad,
      Citas citas,
      AutorizacionCitas autorizacion,
      Clock clock) {
    this.catalogo = Objects.requireNonNull(catalogo);
    this.disponibilidad = Objects.requireNonNull(disponibilidad);
    this.citas = Objects.requireNonNull(citas);
    this.autorizacion = Objects.requireNonNull(autorizacion);
    this.clock = Objects.requireNonNull(clock);
  }

  public List<HorarioCita> ejecutar(
      UUID actorId,
      LocalDate fecha,
      UUID pacienteId,
      UUID profesionalId,
      UUID servicioId,
      UUID consultorioId,
      UUID excluirCitaId) {
    var perfil = autorizacion.perfil(actorId);
    if (!perfil.puede(OperacionCita.PROGRAMAR)) throw new AccesoCitasDenegado();
    UUID filtroProfesional = ConsultarAtencionCitas.alcanceProfesional(perfil, profesionalId);
    var referencias = referencias(pacienteId, filtroProfesional, servicioId, consultorioId);
    if (fecha == null) throw new IllegalArgumentException("La fecha es obligatoria");
    if (!referencias.paciente.activo()
        || !referencias.profesional.activo()
        || !referencias.servicio.activo()
        || !referencias.consultorio.activo()) throw new ReferenciaCitaNoEncontrada("recurso");
    int minutos = referencias.servicio.duracionMinutos();
    if (minutos <= 0 || referencias.servicio.modalidades().isEmpty())
      throw new ReferenciaCitaNoEncontrada("modalidad");
    Instant ahora = clock.instant();
    return disponibilidad
        .intervalosBase(fecha, filtroProfesional, servicioId, consultorioId)
        .stream()
        .flatMap(base -> candidatos(base, minutos).stream())
        .filter(horario -> horario.inicio().isAfter(ahora))
        .filter(
            horario ->
                !citas.existeConflicto(
                    pacienteId, filtroProfesional, consultorioId, horario, excluirCitaId))
        .toList();
  }

  private List<HorarioCita> candidatos(HorarioCita base, int minutos) {
    var resultado = new java.util.ArrayList<HorarioCita>();
    var paso = Duration.ofMinutes(15);
    var local = base.inicio().atZone(ZONA_CENTRO);
    var alineado = local.withSecond(0).withNano(0);
    if (alineado.isBefore(local)) alineado = alineado.plusMinutes(1);
    if (alineado.getMinute() % 15 != 0)
      alineado = alineado.plusMinutes(15 - alineado.getMinute() % 15);
    for (Instant inicio = alineado.toInstant();
        !inicio.plus(Duration.ofMinutes(minutos)).isAfter(base.fin());
        inicio = inicio.plus(paso)) {
      resultado.add(new HorarioCita(inicio, inicio.plus(Duration.ofMinutes(minutos))));
    }
    return resultado;
  }

  private Referencias referencias(
      UUID pacienteId, UUID profesionalId, UUID servicioId, UUID consultorioId) {
    if (pacienteId == null) throw new ReferenciaCitaNoEncontrada("paciente");
    var paciente =
        catalogo.paciente(pacienteId).orElseThrow(() -> new ReferenciaCitaNoEncontrada("paciente"));
    var profesional =
        catalogo
            .profesional(profesionalId)
            .orElseThrow(() -> new ReferenciaCitaNoEncontrada("profesional"));
    var servicio =
        catalogo.servicio(servicioId).orElseThrow(() -> new ReferenciaCitaNoEncontrada("servicio"));
    var consultorio =
        catalogo
            .consultorio(consultorioId)
            .orElseThrow(() -> new ReferenciaCitaNoEncontrada("consultorio"));
    return new Referencias(paciente, profesional, servicio, consultorio);
  }

  private record Referencias(
      CatalogoCitas.Paciente paciente,
      CatalogoCitas.Profesional profesional,
      CatalogoCitas.Servicio servicio,
      CatalogoCitas.Consultorio consultorio) {}
}
