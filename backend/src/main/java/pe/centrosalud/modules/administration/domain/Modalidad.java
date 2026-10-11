package pe.centrosalud.modules.administration.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/** Condición económica de un servicio. La tarifa está en soles con exactamente dos decimales. */
public record Modalidad(
    UUID id, String nombre, boolean conCargo, BigDecimal tarifa, boolean activa) {
  private static final BigDecimal TARIFA_MAXIMA = new BigDecimal("99999999.99");

  public Modalidad {
    Objects.requireNonNull(id);
    nombre = Servicio.texto(nombre, 60);
    if (tarifa == null
        || tarifa.signum() < 0
        || tarifa.compareTo(TARIFA_MAXIMA) > 0
        || tarifa.stripTrailingZeros().scale() > 2) {
      // 40.555 se rechaza en lugar de redondearse: el importe debe ser el que escribió el
      // administrador.
      throw new IllegalArgumentException("La tarifa no es válida");
    }
    tarifa = tarifa.setScale(2, RoundingMode.UNNECESSARY);
    if (conCargo != (tarifa.signum() > 0)) {
      throw new IllegalArgumentException(
          "Una modalidad con cargo requiere tarifa mayor que cero; sin cargo, tarifa cero");
    }
  }
}
