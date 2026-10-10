package pe.centrosalud.modules.identity.domain;

import java.util.Objects;
import java.util.UUID;

/** Identidad de acceso; no contiene contraseñas ni detalles de persistencia. */
public record Cuenta(UUID id, String identificador, boolean activa) {
    public Cuenta {
        Objects.requireNonNull(id, "La cuenta debe tener un ID");
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException("La cuenta debe tener un identificador");
        }
    }
}
