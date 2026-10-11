package pe.centrosalud.modules.appointments.application.ports;

import pe.centrosalud.modules.appointments.domain.Cita;

/** Consulta de bloqueos pendientes, reemplazable por disponibilidad real. */
public interface ConflictosAtencion {
    boolean existeConflictoPendiente(Cita cita);
}
