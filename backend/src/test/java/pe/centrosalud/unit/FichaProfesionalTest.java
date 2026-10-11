package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.administration.domain.FichaProfesional;

class FichaProfesionalTest {
  private static FichaProfesional ficha(
      String identificacion, String nombre, String funcion, Set<UUID> servicios) {
    return new FichaProfesional(
        UUID.randomUUID(), identificacion, nombre, funcion, true, 0, servicios);
  }

  @Test
  void identificacionNombreYFuncionSonObligatoriosYSeRecortan() {
    var f = ficha(" P-01 ", " Profesional de prueba ", " Medicina general ", null);
    assertEquals("P-01", f.identificacion());
    assertEquals("Profesional de prueba", f.nombre());
    assertEquals("Medicina general", f.funcion());
    for (String invalido : new String[] {null, "", "   ", "x".repeat(51)}) {
      assertThrows(
          IllegalArgumentException.class, () -> ficha(invalido, "Nombre", "Funcion", null));
    }
    for (String invalido : new String[] {null, "", "   ", "x".repeat(151)}) {
      assertThrows(IllegalArgumentException.class, () -> ficha("P-01", invalido, "Funcion", null));
    }
    for (String invalido : new String[] {null, "", "   ", "x".repeat(101)}) {
      assertThrows(IllegalArgumentException.class, () -> ficha("P-01", "Nombre", invalido, null));
    }
  }

  @Test
  void rechazaCaracteresDeControl() {
    assertThrows(IllegalArgumentException.class, () -> ficha("P-01", "Nombre\t", "Funcion", null));
  }

  @Test
  void serviciosHabilitadosPuedenEstarVacios() {
    assertEquals(Set.of(), ficha("P-01", "Nombre", "Funcion", null).serviciosHabilitados());
    assertEquals(Set.of(), ficha("P-01", "Nombre", "Funcion", Set.of()).serviciosHabilitados());
    UUID servicio = UUID.randomUUID();
    assertEquals(
        Set.of(servicio),
        ficha("P-01", "Nombre", "Funcion", Set.of(servicio)).serviciosHabilitados());
  }
}
