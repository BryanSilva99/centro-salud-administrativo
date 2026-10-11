package pe.centrosalud.modules.appointments.application.usecases;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.ports.OperacionCita;
import pe.centrosalud.modules.appointments.domain.TipoCierreCita;

public final class RegistrarAusenciaCita {
  public record Comando(UUID actorId, UUID citaId, long version) {}

  private final Citas citas;
  private final ConsultarAgendaCitas agenda;
  private final AutorizacionCitas autorizacion;
  private final Clock clock;

  public RegistrarAusenciaCita(
      Citas citas, ConsultarAgendaCitas agenda, AutorizacionCitas autorizacion, Clock clock) {
    this.citas = Objects.requireNonNull(citas);
    this.agenda = Objects.requireNonNull(agenda);
    this.autorizacion = Objects.requireNonNull(autorizacion);
    this.clock = Objects.requireNonNull(clock);
  }

  public CitaDetalle ejecutar(Comando comando) {
    Objects.requireNonNull(comando);
    autorizacion.verificar(comando.actorId(), OperacionCita.REGISTRAR_AUSENCIA);
    var cita = citas.buscarPorId(comando.citaId()).orElseThrow(CitaNoEncontrada::new);
    if (cita.tipoCierre() != TipoCierreCita.AUSENCIA && cita.version() != comando.version())
      throw new VersionCitaObsoleta();
    try {
      cita.marcarAusente(comando.actorId(), comando.version(), clock);
    } catch (IllegalArgumentException error) {
      throw new OperacionCitaInvalida(error.getMessage());
    }
    return agenda.detalle(citas.guardarTransicion(cita));
  }
}
