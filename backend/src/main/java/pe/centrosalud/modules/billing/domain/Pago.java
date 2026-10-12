package pe.centrosalud.modules.billing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Pago registrado según RF-21 y RN-13. Puede ser parcial o total; vinculado a una sesión de caja
 * abierta.
 */
public record Pago(
    UUID id,
    UUID cargoId,
    UUID sesionCajaId,
    BigDecimal importe,
    MedioPago medio,
    String referencia,
    TipoMovimiento tipo,
    Instant fechaRegistro) {
  public Pago {
    Objects.requireNonNull(id, "El pago debe tener un ID");
    Objects.requireNonNull(cargoId, "El pago debe estar vinculado a un cargo");
    Objects.requireNonNull(sesionCajaId, "El pago debe estar vinculado a una sesión de caja");
    Objects.requireNonNull(importe, "El pago debe tener un importe");
    Objects.requireNonNull(medio, "El pago debe tener un medio");
    Objects.requireNonNull(tipo, "El pago debe tener un tipo");
    Objects.requireNonNull(fechaRegistro, "El pago debe tener fecha de registro");
    if (importe.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("El importe del pago debe ser positivo");
    }
  }

  public boolean esCobro() {
    return tipo == TipoMovimiento.COBRO;
  }

  public boolean esDevolucion() {
    return tipo == TipoMovimiento.DEVOLUCION;
  }
}
