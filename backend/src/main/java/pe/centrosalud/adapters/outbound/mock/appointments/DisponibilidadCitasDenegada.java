package pe.centrosalud.adapters.outbound.mock.appointments;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.DisponibilidadCitas;
import pe.centrosalud.modules.appointments.application.usecases.AccesoCitasDenegado;
import pe.centrosalud.modules.appointments.domain.HorarioCita;

public final class DisponibilidadCitasDenegada implements DisponibilidadCitas {
    @Override public List<HorarioCita> intervalosBase(LocalDate fecha, UUID profesionalId, UUID servicioId, UUID consultorioId) {
        throw new AccesoCitasDenegado();
    }
}
