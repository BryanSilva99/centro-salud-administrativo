package pe.centrosalud.modules.identity.application.ports;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import pe.centrosalud.modules.identity.application.roles.CuentaRoles;
import pe.centrosalud.modules.identity.domain.Rol;

/** Operaciones ejecutadas bajo una misma transacción y bloqueo de administración. */
public interface UnidadAcceso {
  <T> T ejecutar(Function<Operacion, T> accion);

  interface Operacion {
    CuentaRoles cargar(UUID id);

    CuentaRoles crear(UUID actor, String identificador, String hash, Set<Rol> roles);

    CuentaRoles actualizar(UUID actor, CuentaRoles anterior, String identificador, boolean activa);

    boolean profesionalOcupado(UUID profesionalId, UUID cuentaId);

    CuentaRoles vincular(UUID actor, CuentaRoles anterior, UUID profesionalId);

    boolean existeOtroAdministradorActivo(UUID id);

    CuentaRoles guardar(UUID actor, CuentaRoles anterior, Set<Rol> nuevos);
  }
}
