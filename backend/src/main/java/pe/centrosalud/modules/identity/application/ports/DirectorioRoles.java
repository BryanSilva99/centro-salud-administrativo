package pe.centrosalud.modules.identity.application.ports;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import pe.centrosalud.modules.identity.application.roles.CuentaRoles;
import pe.centrosalud.modules.identity.domain.Rol;

public interface DirectorioRoles {
  Pagina<CuentaRoles> listar(String busqueda, int pagina);

  CuentaRoles cargar(UUID id);

  Pagina<Cambio> historial(UUID id, int pagina);

  record Pagina<T>(List<T> elementos, boolean hayMas) {}

  record Cambio(
      UUID id, UUID actorId, UUID cuentaId, Set<Rol> anteriores, Set<Rol> nuevos, Instant fecha) {}
}
