package pe.centrosalud.modules.identity.domain;

import java.time.Duration;
import java.time.Instant;

public final class VigenciaSesion {
    public static final Duration INACTIVIDAD = Duration.ofMinutes(30);
    public static final Duration ABSOLUTA = Duration.ofHours(8);
    private VigenciaSesion() {}
    public static boolean vigente(Instant inicio, Instant actividad, Instant ahora) {
        return !inicio.isAfter(ahora) && !actividad.isAfter(ahora)
                && ahora.isBefore(inicio.plus(ABSOLUTA))
                && ahora.isBefore(actividad.plus(INACTIVIDAD));
    }
}
