package pe.centrosalud.modules.appointments.application.usecases;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.application.ports.OperacionCita;
import pe.centrosalud.modules.appointments.domain.EstadoCita;

public final class ConsultarDashboardCitas {
  public record Resultado(
      LocalDate fecha,
      UUID profesionalId,
      UUID servicioId,
      UUID consultorioId,
      Instant actualizadoEn,
      Map<EstadoCita, Long> porEstado,
      long total,
      long enEspera,
      long atendidas,
      long ausentes) {}

  private final Citas citas;
  private final AutorizacionCitas autorizacion;
  private final Clock clock;

  public ConsultarDashboardCitas(Citas citas, AutorizacionCitas autorizacion, Clock clock) {
    this.citas = Objects.requireNonNull(citas);
    this.autorizacion = Objects.requireNonNull(autorizacion);
    this.clock = Objects.requireNonNull(clock);
  }

  public Resultado ejecutar(
      UUID actorId, LocalDate fecha, UUID profesionalId, UUID servicioId, UUID consultorioId) {
    Objects.requireNonNull(fecha, "La fecha es obligatoria");
    var perfil = autorizacion.perfil(actorId);
    if (!perfil.puede(OperacionCita.CONSULTAR_DASHBOARD)) throw new AccesoCitasDenegado();
    UUID filtroProfesional = ConsultarAtencionCitas.alcanceProfesional(perfil, profesionalId);
    var conteo = new EnumMap<EstadoCita, Long>(EstadoCita.class);
    var agrupado = citas.conteo(fecha, filtroProfesional, servicioId, consultorioId);
    for (var estado : EstadoCita.values()) conteo.put(estado, agrupado.getOrDefault(estado, 0L));
    long total = conteo.values().stream().mapToLong(Long::longValue).sum();
    return new Resultado(
        fecha,
        filtroProfesional,
        servicioId,
        consultorioId,
        clock.instant(),
        Map.copyOf(conteo),
        total,
        conteo.get(EstadoCita.EN_ESPERA),
        conteo.get(EstadoCita.ATENDIDA),
        conteo.get(EstadoCita.AUSENTE));
  }
}
