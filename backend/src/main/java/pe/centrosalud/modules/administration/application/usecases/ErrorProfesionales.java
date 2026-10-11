package pe.centrosalud.modules.administration.application.usecases;

public final class ErrorProfesionales extends RuntimeException {
  public enum Codigo {
    NO_EXISTE,
    CONFLICTO,
    SERVICIO_INVALIDO
  }

  private final Codigo codigo;

  public ErrorProfesionales(Codigo codigo) {
    super(codigo.name());
    this.codigo = codigo;
  }

  public Codigo codigo() {
    return codigo;
  }
}
