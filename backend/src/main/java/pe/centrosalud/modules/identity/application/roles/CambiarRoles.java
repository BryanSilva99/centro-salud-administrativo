package pe.centrosalud.modules.identity.application.roles;
import java.util.Set;
import java.util.UUID;
import pe.centrosalud.modules.identity.application.ports.UnidadAcceso;
import pe.centrosalud.modules.identity.domain.Rol;
import static pe.centrosalud.modules.identity.application.roles.ErrorRoles.Codigo.*;
public final class CambiarRoles {
    private final UnidadAcceso unidad;
    public CambiarRoles(UnidadAcceso unidad) { this.unidad = unidad; }
    public CuentaRoles ejecutar(UUID actor, UUID destinatario, Set<Rol> roles, long version) {
        Set<Rol> nuevos = Set.copyOf(roles);
        return unidad.ejecutar(operacion -> {
            // Reautorizar después de adquirir el bloqueo: otro administrador pudo retirar ADM.
            var autor = operacion.cargar(actor);
            if (!autor.activa() || !autor.roles().contains(Rol.ADM)) throw new ErrorRoles(PROHIBIDO);
            var cuenta = operacion.cargar(destinatario);
            if (cuenta.version() != version) throw new ErrorRoles(CONFLICTO);
            if (cuenta.profesionalId() != null && !nuevos.contains(Rol.PRO)) throw new ErrorRoles(REQUIERE_PRO);
            if (cuenta.roles().equals(nuevos)) return cuenta;
            if (cuenta.activa() && cuenta.roles().contains(Rol.ADM) && !nuevos.contains(Rol.ADM)
                    && !operacion.existeOtroAdministradorActivo(destinatario)) throw new ErrorRoles(ULTIMO_ADMIN);
            return operacion.guardar(actor, cuenta, nuevos);
        });
    }
}
