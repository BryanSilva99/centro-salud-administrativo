package pe.centrosalud.adapters.outbound.modules.billing;

import java.util.UUID;
import pe.centrosalud.modules.billing.application.ports.AccesoCaja;
import pe.centrosalud.modules.billing.application.usecases.AccesoCajaDenegado;
import pe.centrosalud.modules.identity.application.ports.DirectorioRoles;
import pe.centrosalud.modules.identity.domain.Rol;

public record AccesoCajaIdentidad(DirectorioRoles cuentas) implements AccesoCaja {
  public Perfil perfil(UUID actor) {
    var c = cuentas.cargar(actor);
    if (!c.activa() || (!c.roles().contains(Rol.CAJ) && !c.roles().contains(Rol.ADM)))
      throw new AccesoCajaDenegado();
    return new Perfil(c.roles().contains(Rol.CAJ), c.roles().contains(Rol.ADM));
  }
}
