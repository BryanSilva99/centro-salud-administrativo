package pe.centrosalud.modules.identity.application.ports;

import java.util.Set;
import java.util.UUID;
import pe.centrosalud.modules.identity.domain.Rol;

public interface ConsultarRoles {
    Set<Rol> deCuenta(UUID id);
}
