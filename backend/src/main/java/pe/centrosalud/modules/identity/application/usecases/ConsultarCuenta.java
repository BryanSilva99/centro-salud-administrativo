package pe.centrosalud.modules.identity.application.usecases;

import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.identity.application.ports.ConsultarCuentas;
import pe.centrosalud.modules.identity.domain.Cuenta;

/** Consulta administrativa; no verifica credenciales ni concede acceso. */
public final class ConsultarCuenta {
  private final ConsultarCuentas cuentas;

  public ConsultarCuenta(ConsultarCuentas cuentas) {
    this.cuentas = Objects.requireNonNull(cuentas);
  }

  public Cuenta ejecutar(UUID id) {
    Objects.requireNonNull(id, "La consulta requiere un ID");
    return cuentas.buscarPorId(id).orElseThrow(CuentaNoEncontrada::new);
  }
}
