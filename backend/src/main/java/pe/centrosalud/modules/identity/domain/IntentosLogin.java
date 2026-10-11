package pe.centrosalud.modules.identity.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Cinco fallos dentro de una ventana móvil de 15 minutos bloquean otros 15 minutos. */
public record IntentosLogin(List<Instant> fallos, Instant bloqueadoHasta) {
  private static final Duration VENTANA = Duration.ofMinutes(15);

  public IntentosLogin {
    fallos = List.copyOf(fallos);
  }

  public static IntentosLogin vacio() {
    return new IntentosLogin(List.of(), null);
  }

  public boolean bloqueado(Instant ahora) {
    return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
  }

  public IntentosLogin fallar(Instant ahora) {
    if (bloqueado(ahora)) return this;
    var vigentes =
        new ArrayList<>(
            fallos.stream().filter(fecha -> fecha.isAfter(ahora.minus(VENTANA))).toList());
    vigentes.add(ahora);
    return vigentes.size() >= 5
        ? new IntentosLogin(List.of(), ahora.plus(VENTANA))
        : new IntentosLogin(vigentes, null);
  }
}
