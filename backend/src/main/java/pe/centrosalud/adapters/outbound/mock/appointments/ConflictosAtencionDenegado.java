package pe.centrosalud.adapters.outbound.mock.appointments;

import pe.centrosalud.modules.appointments.application.ports.ConflictosAtencion;
import pe.centrosalud.modules.appointments.domain.Cita;

public final class ConflictosAtencionDenegado implements ConflictosAtencion {
    @Override public boolean existeConflictoPendiente(Cita cita) { return false; }
}
