package pe.centrosalud.modules.identity.application.ports;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import pe.centrosalud.modules.identity.domain.Rol;

public interface HistorialCuentas {
  DirectorioRoles.Pagina<CambioCuenta> historialCuenta(UUID id, int pagina);

  record CambioCuenta(
      UUID id,
      UUID actorId,
      String tipo,
      String identificadorAnterior,
      String identificadorNuevo,
      Boolean activaAnterior,
      boolean activaNueva,
      Set<Rol> roles,
      Instant fecha,
      UUID profesionalAnterior,
      UUID profesionalNuevo) {}
}
