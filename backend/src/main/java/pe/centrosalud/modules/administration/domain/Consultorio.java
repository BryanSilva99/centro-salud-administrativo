package pe.centrosalud.modules.administration.domain;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Consultorio del centro y los servicios compatibles con él. Un consultorio sin servicios
 * compatibles no puede recibir reservas, pero es un estado válido (a diferencia de Servicio, que
 * exige al menos una modalidad activa).
 */
public record Consultorio(
    UUID id,
    String codigo,
    String nombre,
    boolean activo,
    long version,
    Set<UUID> serviciosCompatibles) {
  public Consultorio {
    Objects.requireNonNull(id);
    codigo = texto(codigo, 20);
    nombre = texto(nombre, 150);
    serviciosCompatibles =
        serviciosCompatibles == null ? Set.of() : Set.copyOf(serviciosCompatibles);
  }

  private static String texto(String valor, int maximo) {
    if (valor == null
        || valor.isBlank()
        || valor.strip().length() > maximo
        || valor.codePoints().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("Los datos del consultorio no son válidos");
    }
    return valor.strip();
  }
}
