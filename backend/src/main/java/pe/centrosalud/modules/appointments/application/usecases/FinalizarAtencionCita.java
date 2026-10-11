package pe.centrosalud.modules.appointments.application.usecases;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.ports.OperacionCita;

public final class FinalizarAtencionCita {
  public record Comando(UUID actorId, UUID citaId, long version) {}

  private final Citas citas;
  private final ConsultarAgendaCitas agenda;
  private final AutorizacionCitas autorizacion;
  private final Clock clock;

  public FinalizarAtencionCita(
      Citas citas, ConsultarAgendaCitas agenda, AutorizacionCitas autorizacion, Clock clock) {
    this.citas = Objects.requireNonNull(citas);
    this.agenda = Objects.requireNonNull(agenda);
    this.autorizacion = Objects.requireNonNull(autorizacion);
    this.clock = Objects.requireNonNull(clock);
  }

  public CitaDetalle ejecutar(Comando comando) {
    Objects.requireNonNull(comando);
    var perfil = autorizacion.perfil(comando.actorId());
    if (!perfil.puede(OperacionCita.FINALIZAR_ATENCION)) throw new AccesoCitasDenegado();
    var cita = citas.buscarPorId(comando.citaId()).orElseThrow(CitaNoEncontrada::new);
    if (perfil.profesionalAsignadoId() == null
        || !perfil.profesionalAsignadoId().equals(cita.profesionalId()))
      throw new AccesoCitasDenegado();
    if (cita.atencionFinalizadaEn() == null) {
      if (cita.version() != comando.version()) throw new VersionCitaObsoleta();
      try {
        cita.finalizarAtencion(comando.actorId(), comando.version(), clock);
      } catch (IllegalArgumentException error) {
        throw new OperacionCitaInvalida(error.getMessage());
      }
      cita = citas.guardarTransicion(cita);
    }
    return agenda.detalle(cita);
  }
}
