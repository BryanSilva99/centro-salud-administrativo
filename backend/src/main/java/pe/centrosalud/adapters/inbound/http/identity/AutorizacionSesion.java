package pe.centrosalud.adapters.inbound.http.identity;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pe.centrosalud.modules.identity.domain.PermisosCuenta;
import pe.centrosalud.modules.identity.domain.Rol;

public final class AutorizacionSesion {
    private AutorizacionSesion() {}
    public static List<SimpleGrantedAuthority> authorities(Set<Rol> roles) {
        return Stream.concat(roles.stream().map(r -> "ROLE_" + r.name()),
                PermisosCuenta.para(roles).stream().map(Enum::name))
                .sorted().map(SimpleGrantedAuthority::new).toList();
    }
}
