package pe.centrosalud.modules.administration.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Ficha administrativa del paciente. El código es null hasta que la persistencia lo asigna. */
public record Paciente(
    UUID id,
    String codigo,
    TipoDocumento tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidos,
    LocalDate fechaNacimiento,
    String telefono,
    String correo,
    long version) {
  private static final LocalDate NACIMIENTO_MINIMO = LocalDate.of(1900, 1, 1);

  public Paciente {
    Objects.requireNonNull(id);
    if (tipoDocumento == null || fechaNacimiento == null) {
      throw new IllegalArgumentException("Faltan datos obligatorios del paciente");
    }
    numeroDocumento = tipoDocumento.normalizar(numeroDocumento);
    nombres = obligatorio(nombres, 100);
    apellidos = obligatorio(apellidos, 100);
    telefono = opcional(telefono, "[0-9+ ()-]{6,20}");
    correo = opcional(correo, "[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    if (correo != null && correo.length() > 254)
      throw new IllegalArgumentException("El correo no es válido");
  }

  /** La fecha actual llega desde la aplicación para que el dominio no dependa de un reloj. */
  public static void validarNacimiento(LocalDate fecha, LocalDate hoy) {
    if (fecha == null || fecha.isAfter(hoy) || fecha.isBefore(NACIMIENTO_MINIMO)) {
      throw new IllegalArgumentException("La fecha de nacimiento no es válida");
    }
  }

  private static String obligatorio(String valor, int maximo) {
    if (valor == null
        || valor.isBlank()
        || valor.strip().length() > maximo
        || valor.codePoints().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("Los nombres y apellidos no son válidos");
    }
    return valor.strip();
  }

  private static String opcional(String valor, String formato) {
    if (valor == null || valor.isBlank()) return null;
    String limpio = valor.strip();
    if (!limpio.matches(formato))
      throw new IllegalArgumentException("Los datos de contacto no son válidos");
    return limpio;
  }
}
