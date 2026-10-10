package pe.centrosalud.adapters.inbound.http.identity;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import pe.centrosalud.modules.identity.domain.Cuenta;

/** Refleja las mismas autoridades utilizadas para autorizar esta solicitud. */
public record SesionResponse(UUID id, String identificador, boolean activa,
                             List<String> roles, List<String> permisos) {
    public static SesionResponse de(Cuenta cuenta, Authentication authentication) {
        var authorities = authentication.getAuthorities().stream().map(a -> a.getAuthority()).sorted().toList();
        return new SesionResponse(cuenta.id(), cuenta.identificador(), cuenta.activa(),
                authorities.stream().filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).toList(),
                authorities.stream().filter(a -> !a.startsWith("ROLE_")).toList());
    }
}
