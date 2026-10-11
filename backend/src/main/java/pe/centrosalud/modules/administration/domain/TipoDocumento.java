package pe.centrosalud.modules.administration.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** Formatos internos de RN-04; validar el formato no confirma la identidad de la persona. */
public enum TipoDocumento {
  DNI("[0-9]{8}"),
  CE("[A-Z0-9]{9}");

  private final Pattern formato;

  TipoDocumento(String formato) {
    this.formato = Pattern.compile(formato);
  }

  public String normalizar(String numero) {
    if (numero == null) throw new IllegalArgumentException("El número de documento es obligatorio");
    // El CE se guarda en mayúsculas para que "ab1234567" y "AB1234567" sean el mismo documento.
    String normalizado = numero.strip().toUpperCase(Locale.ROOT);
    if (!formato.matcher(normalizado).matches()) {
      throw new IllegalArgumentException("El número no corresponde al tipo de documento");
    }
    return normalizado;
  }
}
