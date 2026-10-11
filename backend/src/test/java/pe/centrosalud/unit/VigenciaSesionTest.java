package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.identity.domain.VigenciaSesion;

class VigenciaSesionTest {
  @Test
  void venceEnLosLimitesDeInactividadYTiempoAbsoluto() {
    var inicio = Instant.parse("2026-10-09T12:00:00Z");
    assertTrue(VigenciaSesion.vigente(inicio, inicio, inicio.plusSeconds(1799)));
    assertFalse(VigenciaSesion.vigente(inicio, inicio, inicio.plusSeconds(1800)));
    assertFalse(
        VigenciaSesion.vigente(inicio, inicio.plusSeconds(28799), inicio.plusSeconds(28800)));
    assertFalse(VigenciaSesion.vigente(inicio, inicio, inicio.minusSeconds(1)));
  }
}
