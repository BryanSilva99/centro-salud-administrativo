package pe.centrosalud.modules.identity.application.ports;

import java.util.Objects;
import pe.centrosalud.modules.identity.domain.Cuenta;

/** Datos internos de verificación. No es una respuesta HTTP. */
public final class CredencialesCuenta {
    private final Cuenta cuenta;
    private final String hash;
    private final long versionAcceso;

    public CredencialesCuenta(Cuenta cuenta, String hash) {
        this(cuenta, hash, 0);
    }

    public CredencialesCuenta(Cuenta cuenta, String hash, long versionAcceso) {
        this.versionAcceso = versionAcceso;
        this.cuenta = Objects.requireNonNull(cuenta);
        this.hash = hash; // Una cuenta todavía sin credencial no puede autenticarse.
    }

    public Cuenta cuenta() { return cuenta; }
    public long versionAcceso() { return versionAcceso; }
    public String hash() { return hash; }

    @Override public String toString() { return "CredencialesCuenta[contenido protegido]"; }
}
