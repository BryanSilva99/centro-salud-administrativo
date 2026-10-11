package pe.centrosalud.modules.administration.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Servicio del catálogo y sus modalidades económicas. Los profesionales y consultorios habilitados
 * se agregarán con RF-07 y RF-09.
 */
public record Servicio(
    UUID id,
    String nombre,
    int duracionMinutos,
    boolean activo,
    long version,
    List<Modalidad> modalidades) {
  public Servicio {
    Objects.requireNonNull(id);
    nombre = texto(nombre, 100);
    if (duracionMinutos < 1 || duracionMinutos > 480)
      throw new IllegalArgumentException("La duración no es válida");
    if (modalidades == null || modalidades.isEmpty())
      throw new IllegalArgumentException("Se requiere al menos una modalidad");
    modalidades = List.copyOf(modalidades);
    var nombres = new HashSet<String>();
    for (Modalidad modalidad : modalidades) {
      if (!nombres.add(modalidad.nombre().toLowerCase(Locale.ROOT))) {
        throw new IllegalArgumentException(
            "Las modalidades de un servicio no pueden repetir nombre");
      }
    }
    // Sin una modalidad activa, una reserva no tendría importe aplicable (RF-08/CA-2).
    if (activo && modalidades.stream().noneMatch(Modalidad::activa)) {
      throw new IllegalArgumentException("Un servicio activo requiere una modalidad activa");
    }
  }

  static String texto(String valor, int maximo) {
    if (valor == null
        || valor.isBlank()
        || valor.strip().length() > maximo
        || valor.codePoints().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("Los datos del servicio no son válidos");
    }
    return valor.strip();
  }
}
