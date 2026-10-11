package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.identity.domain.Cuenta;

class CuentaTest {
  @Test
  void rechazaIdentidadAusente() {
    assertThrows(NullPointerException.class, () -> new Cuenta(null, "bryan", true));
  }

  @Test
  void rechazaIdentificadoresVacios() {
    for (String identificador : new String[] {null, "", " ", "\t\n"}) {
      assertThrows(
          IllegalArgumentException.class, () -> new Cuenta(UUID.randomUUID(), identificador, true));
    }
  }

  @Test
  void unaCuentaInactivaSigueSiendoUnaCuentaValida() {
    assertFalse(new Cuenta(UUID.randomUUID(), "bryan", false).activa());
  }
}
