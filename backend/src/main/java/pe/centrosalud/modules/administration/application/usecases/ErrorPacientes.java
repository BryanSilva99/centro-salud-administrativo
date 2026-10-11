package pe.centrosalud.modules.administration.application.usecases;

/** Reemplaza a PacienteDuplicado: RF-05 añade CONFLICTO y NO_EXISTE a la actualización. */
public final class ErrorPacientes extends RuntimeException {
  public enum Codigo {
    NO_EXISTE,
    CONFLICTO,
    DOCUMENTO_DUPLICADO
  }

  private final Codigo codigo;

  // No incluye el documento: el mensaje puede terminar en respuestas o registros.
  public ErrorPacientes(Codigo codigo) {
    super(codigo.name());
    this.codigo = codigo;
  }

  public Codigo codigo() {
    return codigo;
  }
}
