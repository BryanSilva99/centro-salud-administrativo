package pe.centrosalud.modules.billing.application.usecases;

import java.math.BigDecimal;

public final class Dinero {
  private Dinero() {}

  public static void validar(BigDecimal valor) {
    if (valor == null
        || valor.signum() < 0
        || valor.stripTrailingZeros().scale() > 2
        || valor.compareTo(new BigDecimal("9999999999.99")) > 0)
      throw new IllegalArgumentException(
          "El importe debe ser no negativo, en soles y con máximo dos decimales");
  }
}
