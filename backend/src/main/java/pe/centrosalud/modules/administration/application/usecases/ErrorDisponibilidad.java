package pe.centrosalud.modules.administration.application.usecases;

public final class ErrorDisponibilidad extends RuntimeException {
  public enum Codigo {
    NO_EXISTE,
    RECURSO_INVALIDO,
    SUPERPUESTO
  }

  private final Codigo codigo;

  public ErrorDisponibilidad(Codigo codigo) {
    super(codigo.name());
    this.codigo = codigo;
  }

  public Codigo codigo() {
    return codigo;
  }
}
