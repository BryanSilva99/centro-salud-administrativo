package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.administration.domain.Consultorio;

class ConsultorioTest {
  private static Consultorio consultorio(String codigo, String nombre, Set<UUID> servicios) {
    return new Consultorio(UUID.randomUUID(), codigo, nombre, true, 0, servicios);
  }

  @Test
  void codigoYNombreSonObligatoriosYSeRecortan() {
    var c = consultorio(" C-101 ", " Consultorio 1 ", null);
    assertEquals("C-101", c.codigo());
    assertEquals("Consultorio 1", c.nombre());
    for (String invalido : new String[] {null, "", "   ", "x".repeat(21)}) {
      assertThrows(IllegalArgumentException.class, () -> consultorio(invalido, "Nombre", null));
    }
    for (String invalido : new String[] {null, "", "   ", "x".repeat(151)}) {
      assertThrows(IllegalArgumentException.class, () -> consultorio("C-1", invalido, null));
    }
  }

  @Test
  void codigoRechazaCaracteresDeControl() {
    assertThrows(IllegalArgumentException.class, () -> consultorio("C-1\t", "Nombre", null));
  }

  @Test
  void serviciosCompatiblesPuedenEstarVacios() {
    assertEquals(Set.of(), consultorio("C-1", "Nombre", null).serviciosCompatibles());
    assertEquals(Set.of(), consultorio("C-1", "Nombre", Set.of()).serviciosCompatibles());
    UUID servicio = UUID.randomUUID();
    assertEquals(
        Set.of(servicio), consultorio("C-1", "Nombre", Set.of(servicio)).serviciosCompatibles());
  }
}
