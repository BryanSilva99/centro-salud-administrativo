package pe.centrosalud.modules.identity.application.ports;

import java.util.Optional;
import java.util.UUID;
import pe.centrosalud.modules.identity.domain.Cuenta;

/** Contrato que necesita la aplicación; no conoce SQL ni JPA. */
public interface ConsultarCuentas {
  Optional<Cuenta> buscarPorId(UUID id);
}
