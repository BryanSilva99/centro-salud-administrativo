package pe.centrosalud.modules.billing.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Sesión de caja según RF-20, RF-24 y RN-15. Una sola sesión abierta por cajero; cierre inmutable.
 */
public record SesionCaja(
    UUID id,
    UUID cajeroId,
    BigDecimal fondoInicial,
    EstadoSesionCaja estado,
    Instant fechaApertura,
    Instant fechaCierre,
    BigDecimal efectivoDeclarado,
    BigDecimal diferencia,
    String motivoDiferencia) {
  public SesionCaja {
    Objects.requireNonNull(id, "La sesión debe tener un ID");
    Objects.requireNonNull(cajeroId, "La sesión debe tener un cajero");
    Objects.requireNonNull(fondoInicial, "La sesión debe tener fondo inicial");
    Objects.requireNonNull(estado, "La sesión debe tener un estado");
    Objects.requireNonNull(fechaApertura, "La sesión debe tener fecha de apertura");
    if (fondoInicial.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("El fondo inicial no puede ser negativo");
    }
  }

  public boolean estaAbierta() {
    return estado == EstadoSesionCaja.ABIERTA;
  }

  public boolean estaCerrada() {
    return estado == EstadoSesionCaja.CERRADA;
  }
}
