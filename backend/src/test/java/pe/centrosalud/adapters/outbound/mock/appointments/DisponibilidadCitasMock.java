package pe.centrosalud.adapters.outbound.mock.appointments;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.DisponibilidadCitas;
import pe.centrosalud.modules.appointments.application.usecases.ConsultarDisponibilidadCitas;
import pe.centrosalud.modules.appointments.domain.HorarioCita;

public final class DisponibilidadCitasMock implements DisponibilidadCitas {
  @Override
  public List<HorarioCita> intervalosBase(
      LocalDate fecha, UUID profesionalId, UUID servicioId, UUID consultorioId) {
    if (fecha == null || fecha.getDayOfWeek().getValue() > 5) return List.of();
    boolean primeraLinea =
        CatalogoCitasMock.PROFESIONAL_1.equals(profesionalId)
            && CatalogoCitasMock.SERVICIO_1.equals(servicioId)
            && CatalogoCitasMock.CONSULTORIO_1.equals(consultorioId);
    boolean segundaLinea =
        CatalogoCitasMock.PROFESIONAL_2.equals(profesionalId)
            && CatalogoCitasMock.SERVICIO_2.equals(servicioId)
            && CatalogoCitasMock.CONSULTORIO_2.equals(consultorioId);
    if (!primeraLinea && !segundaLinea) return List.of();
    return List.of(
        intervalo(fecha, LocalTime.of(8, 0), LocalTime.NOON),
        intervalo(fecha, LocalTime.of(14, 0), LocalTime.of(17, 0)));
  }

  private HorarioCita intervalo(LocalDate fecha, LocalTime inicio, LocalTime fin) {
    var zona = ConsultarDisponibilidadCitas.ZONA_CENTRO;
    return new HorarioCita(
        fecha.atTime(inicio).atZone(zona).toInstant(), fecha.atTime(fin).atZone(zona).toInstant());
  }
}
