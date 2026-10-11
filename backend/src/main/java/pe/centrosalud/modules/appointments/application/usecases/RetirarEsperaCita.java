package pe.centrosalud.modules.appointments.application.usecases;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.ports.ImpactoEconomicoCita;
import pe.centrosalud.modules.appointments.application.ports.OperacionCita;
import pe.centrosalud.modules.appointments.domain.TipoCierreCita;

public final class RetirarEsperaCita {
  public record Comando(UUID actorId, UUID citaId, String motivo, long version) {}

  private final Citas citas;
  private final ConsultarAgendaCitas agenda;
  private final AutorizacionCitas autorizacion;
  private final ImpactoEconomicoCita impactoEconomico;
  private final Clock clock;

  public RetirarEsperaCita(
      Citas citas,
      ConsultarAgendaCitas agenda,
      AutorizacionCitas autorizacion,
      ImpactoEconomicoCita impactoEconomico,
      Clock clock) {
    this.citas = Objects.requireNonNull(citas);
    this.agenda = Objects.requireNonNull(agenda);
    this.autorizacion = Objects.requireNonNull(autorizacion);
    this.impactoEconomico = Objects.requireNonNull(impactoEconomico);
    this.clock = Objects.requireNonNull(clock);
  }

  public CierreCitaResultado ejecutar(Comando comando) {
    Objects.requireNonNull(comando);
    autorizacion.verificar(comando.actorId(), OperacionCita.REGISTRAR_RETIRO);
    var cita = citas.buscarPorId(comando.citaId()).orElseThrow(CitaNoEncontrada::new);
    if (cita.tipoCierre() != TipoCierreCita.RETIRO_ESPERA && cita.version() != comando.version())
      throw new VersionCitaObsoleta();
    try {
      cita.retirarDeEspera(comando.actorId(), comando.version(), comando.motivo(), clock);
    } catch (IllegalArgumentException error) {
      throw new OperacionCitaInvalida(error.getMessage());
    }
    cita = citas.guardarTransicion(cita);
    return new CierreCitaResultado(
        agenda.detalle(cita), impactoEconomico.requiereGestion(cita.id()));
  }
}
