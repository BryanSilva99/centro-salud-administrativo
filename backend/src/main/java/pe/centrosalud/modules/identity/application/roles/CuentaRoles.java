package pe.centrosalud.modules.identity.application.roles;
import java.util.Set;
import java.util.UUID;
import pe.centrosalud.modules.identity.domain.Rol;
public record CuentaRoles(UUID id, String identificador, boolean activa, Set<Rol> roles, long version, UUID profesionalId) {
    public CuentaRoles { roles = Set.copyOf(roles); }
}
