package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.identity.domain.IntentosLogin;

class IntentosLoginTest {
  private final Instant ahora = Instant.parse("2026-10-09T12:00:00Z");

  @Test
  void quintoFalloBloqueaDuranteQuinceMinutosSinExtenderPorReintentos() {
    var estado = IntentosLogin.vacio();
    for (int i = 0; i < 4; i++) estado = estado.fallar(ahora);
    assertFalse(estado.bloqueado(ahora));
    estado = estado.fallar(ahora);
    assertTrue(estado.bloqueado(ahora.plusSeconds(899)));
    assertEquals(estado, estado.fallar(ahora.plusSeconds(500)));
    assertFalse(estado.bloqueado(ahora.plusSeconds(900)));
    assertFalse(estado.fallar(ahora.plusSeconds(900)).bloqueado(ahora.plusSeconds(900)));
  }

  @Test
  void usaVentanaMovilYDescartaFallosDeHaceQuinceMinutos() {
    var estado = IntentosLogin.vacio().fallar(ahora);
    for (int i = 0; i < 3; i++) estado = estado.fallar(ahora.plusSeconds(899));
    estado = estado.fallar(ahora.plusSeconds(900));
    assertFalse(estado.bloqueado(ahora.plusSeconds(900)));
    assertEquals(4, estado.fallos().size());
    assertTrue(estado.fallar(ahora.plusSeconds(901)).bloqueado(ahora.plusSeconds(901)));
  }
}
