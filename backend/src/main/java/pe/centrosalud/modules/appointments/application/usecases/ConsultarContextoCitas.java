package pe.centrosalud.modules.appointments.application.usecases;

import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.CatalogoCitas;
import pe.centrosalud.modules.appointments.application.ports.OperacionCita;

public final class ConsultarContextoCitas {
    private final CatalogoCitas catalogo;
    private final AutorizacionCitas autorizacion;

    public ConsultarContextoCitas(CatalogoCitas catalogo, AutorizacionCitas autorizacion) {
        this.catalogo = Objects.requireNonNull(catalogo);
        this.autorizacion = Objects.requireNonNull(autorizacion);
    }

    public ContextoCitas ejecutar(UUID actorId) {
        var perfil = autorizacion.perfil(actorId);
        if (!perfil.puede(OperacionCita.CONSULTAR)) throw new AccesoCitasDenegado();
        return new ContextoCitas(catalogo.contexto(), perfil);
    }
}
