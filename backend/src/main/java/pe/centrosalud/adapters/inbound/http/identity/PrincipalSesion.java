package pe.centrosalud.adapters.inbound.http.identity;

import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

/** Solo ID y versión: no se serializan contraseñas, hashes ni roles obsoletos. */
public record PrincipalSesion(UUID id, long versionAcceso) implements Principal, Serializable {
    private static final long serialVersionUID = 1L;
    @Override public String getName() { return id.toString(); }
}
