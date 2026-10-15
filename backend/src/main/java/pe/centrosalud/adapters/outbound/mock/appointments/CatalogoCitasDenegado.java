package pe.centrosalud.adapters.outbound.mock.appointments;

import java.util.Optional;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.CatalogoCitas;

/** No expone fixtures cuando el modo demo está desactivado. */
public final class CatalogoCitasDenegado implements CatalogoCitas {
    private final RuntimeException error = new pe.centrosalud.modules.appointments.application.usecases.AccesoCitasDenegado();
    @Override public Optional<Paciente> paciente(UUID id) { throw error; }
    @Override public Optional<Profesional> profesional(UUID id) { throw error; }
    @Override public Optional<Servicio> servicio(UUID id) { throw error; }
    @Override public Optional<Consultorio> consultorio(UUID id) { throw error; }
    @Override public Contexto contexto() { throw error; }
}
