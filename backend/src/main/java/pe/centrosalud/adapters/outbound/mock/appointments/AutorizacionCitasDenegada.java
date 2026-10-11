package pe.centrosalud.adapters.outbound.mock.appointments;

import java.util.UUID;
import pe.centrosalud.modules.appointments.application.ports.AutorizacionCitas;
import pe.centrosalud.modules.appointments.application.ports.PerfilCitas;
import pe.centrosalud.modules.appointments.application.usecases.AccesoCitasDenegado;

public final class AutorizacionCitasDenegada implements AutorizacionCitas {
    @Override public PerfilCitas perfil(UUID cuentaId) { throw new AccesoCitasDenegado(); }
}
