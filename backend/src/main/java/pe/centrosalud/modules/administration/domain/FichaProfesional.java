package pe.centrosalud.modules.administration.domain;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Identificación del profesional, sus servicios habilitados y su estado. */
public record FichaProfesional(
    UUID id,
    String identificacion,
    String nombre,
    String funcion,
    boolean activa,
    long version,
    Set<UUID> serviciosHabilitados) {
  public FichaProfesional {
    Objects.requireNonNull(id);
    identificacion = texto(identificacion, 50);
    nombre = texto(nombre, 150);
    funcion = texto(funcion, 100);
    serviciosHabilitados =
        serviciosHabilitados == null ? Set.of() : Set.copyOf(serviciosHabilitados);
  }

  private static String texto(String valor, int maximo) {
    if (valor == null
        || valor.isBlank()
        || valor.strip().length() > maximo
        || valor.codePoints().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("Los datos de la ficha no son válidos");
    }
    return valor.strip();
  }
}
