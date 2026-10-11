package pe.centrosalud.modules.appointments.application.ports;

import java.util.Set;
import java.util.UUID;

/** Capacidades que identidad deberá reemplazar por permisos reales. */
public record PerfilCitas(Set<OperacionCita> operaciones, UUID profesionalAsignadoId) {
    public PerfilCitas {
        operaciones = Set.copyOf(operaciones);
    }

    public boolean puede(OperacionCita operacion) {
        return operaciones.contains(operacion);
    }

    public boolean esProfesional() {
        return puede(OperacionCita.INICIAR_ATENCION) || puede(OperacionCita.FINALIZAR_ATENCION);
    }
}
