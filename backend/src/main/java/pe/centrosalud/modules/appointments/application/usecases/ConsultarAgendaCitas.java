package pe.centrosalud.modules.appointments.application.usecases;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.CatalogoCitas;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.ports.OperacionCita;

public final class ConsultarAgendaCitas {
  private final Citas citas;
  private final CatalogoCitas catalogo;
  private final AutorizacionCitas autorizacion;

  public ConsultarAgendaCitas(Citas citas, CatalogoCitas catalogo, AutorizacionCitas autorizacion) {
    this.citas = Objects.requireNonNull(citas);
    this.catalogo = Objects.requireNonNull(catalogo);
    this.autorizacion = Objects.requireNonNull(autorizacion);
  }

  public List<CitaDetalle> ejecutar(
      UUID actorId, LocalDate fecha, UUID profesionalId, UUID servicioId, UUID consultorioId) {
    if (fecha == null) throw new IllegalArgumentException("La fecha es obligatoria");
    var perfil = autorizacion.perfil(actorId);
    if (!perfil.puede(OperacionCita.CONSULTAR)) throw new AccesoCitasDenegado();
    UUID filtroProfesional = ConsultarAtencionCitas.alcanceProfesional(perfil, profesionalId);
    return citas.agenda(fecha, filtroProfesional, servicioId, consultorioId).stream()
        .map(this::detalle)
        .toList();
  }

  CitaDetalle detalle(pe.centrosalud.modules.appointments.domain.Cita cita) {
    var paciente =
        catalogo
            .paciente(cita.pacienteId())
            .orElseThrow(() -> new ReferenciaCitaNoEncontrada("paciente"));
    var profesional =
        catalogo
            .profesional(cita.profesionalId())
            .orElseThrow(() -> new ReferenciaCitaNoEncontrada("profesional"));
    var servicio =
        catalogo
            .servicio(cita.servicioId())
            .orElseThrow(() -> new ReferenciaCitaNoEncontrada("servicio"));
    var consultorio =
        catalogo
            .consultorio(cita.consultorioId())
            .orElseThrow(() -> new ReferenciaCitaNoEncontrada("consultorio"));
    return new CitaDetalle(cita, paciente, profesional, servicio, consultorio);
  }
}
