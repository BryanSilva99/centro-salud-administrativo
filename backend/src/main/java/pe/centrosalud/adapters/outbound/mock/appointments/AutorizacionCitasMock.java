package pe.centrosalud.adapters.outbound.mock.appointments;

import java.util.EnumSet;
import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.OperacionCita;
import pe.centrosalud.modules.appointments.application.ports.PerfilCitas;

/** En modo demo toda cuenta autenticada representa recepción y al profesional demo 01. */
public final class AutorizacionCitasMock implements AutorizacionCitas {
    @Override public PerfilCitas perfil(UUID cuentaId) {
        if (cuentaId == null) throw new IllegalArgumentException("Actor inválido");
        return new PerfilCitas(EnumSet.allOf(OperacionCita.class), CatalogoCitasMock.PROFESIONAL_1);
    }
}
