package pe.centrosalud.modules.identity.application.ports;

import java.util.Optional;
import java.util.UUID;

public interface ConsultarEstadoAcceso {
  Optional<Estado> buscarEstado(UUID id);

  record Estado(boolean activa, long version) {}
}
