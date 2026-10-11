package pe.centrosalud.modules.administration.application.usecases;

public final class ErrorConsultorios extends RuntimeException {
  public enum Codigo {
    NO_EXISTE,
    CONFLICTO,
    CODIGO_DUPLICADO,
    SERVICIO_INVALIDO
  }

  private final Codigo codigo;

  public ErrorConsultorios(Codigo codigo) {
    super(codigo.name());
    this.codigo = codigo;
  }

  public Codigo codigo() {
    return codigo;
  }
}
