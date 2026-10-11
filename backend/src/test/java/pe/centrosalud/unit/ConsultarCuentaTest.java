package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.identity.application.usecases.*;
import pe.centrosalud.modules.identity.domain.Cuenta;

class ConsultarCuentaTest {
  @Test
  void encuentraCuentaSinDependerDeSpringOPostgres() {
    var cuenta = new Cuenta(UUID.randomUUID(), "bryan", false);
    var caso =
        new ConsultarCuenta(id -> id.equals(cuenta.id()) ? Optional.of(cuenta) : Optional.empty());
    assertEquals(cuenta, caso.ejecutar(cuenta.id()));
  }

  @Test
  void ausenciaEsUnResultadoExplicito() {
    var caso = new ConsultarCuenta(id -> Optional.empty());
    assertThrows(CuentaNoEncontrada.class, () -> caso.ejecutar(UUID.randomUUID()));
  }

  @Test
  void rechazaIdNuloAntesDeConsultar() {
    var caso =
        new ConsultarCuenta(
            id -> {
              fail("No debe consultar sin ID");
              return Optional.empty();
            });
    assertThrows(NullPointerException.class, () -> caso.ejecutar(null));
  }
}
