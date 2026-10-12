package pe.centrosalud.modules.billing.application.ports;

import java.util.UUID;

public interface AccesoCaja {
  Perfil perfil(UUID actor);

  record Perfil(boolean cajero, boolean administrador) {}
}
