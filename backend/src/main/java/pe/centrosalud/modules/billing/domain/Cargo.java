package pe.centrosalud.modules.billing.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Cargo por servicio según RF-19 y RN-13. Un cargo vigente por cita; importe en PEN con dos
 * decimales.
 */
public record Cargo(UUID id, UUID citaId, UUID pacienteId, BigDecimal importe, EstadoCargo estado) {
  public Cargo {
    Objects.requireNonNull(id, "El cargo debe tener un ID");
    Objects.requireNonNull(citaId, "El cargo debe estar vinculado a una cita");
    Objects.requireNonNull(pacienteId, "El cargo debe estar vinculado a un paciente");
    Objects.requireNonNull(importe, "El cargo debe tener un importe");
    Objects.requireNonNull(estado, "El cargo debe tener un estado");
    if (importe.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("El importe no puede ser negativo");
    }
  }

  public boolean estaVigente() {
    return estado != EstadoCargo.ANULADO;
  }

  public boolean estaAnulado() {
    return estado == EstadoCargo.ANULADO;
  }
}
