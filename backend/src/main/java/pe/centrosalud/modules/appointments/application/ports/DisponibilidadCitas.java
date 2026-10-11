package pe.centrosalud.modules.appointments.application.ports;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import pe.centrosalud.modules.appointments.domain.HorarioCita;

/** Disponibilidad configurada de profesionales y consultorios. */
public interface DisponibilidadCitas {
    List<HorarioCita> intervalosBase(LocalDate fecha, UUID profesionalId, UUID servicioId, UUID consultorioId);
}
