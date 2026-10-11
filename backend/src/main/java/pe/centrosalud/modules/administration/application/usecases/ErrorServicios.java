package pe.centrosalud.modules.administration.application.usecases;

public final class ErrorServicios extends RuntimeException {
    public enum Codigo { NO_EXISTE, CONFLICTO, NOMBRE_DUPLICADO, MODALIDAD_INVALIDA }
    private final Codigo codigo;
    public ErrorServicios(Codigo codigo) { super(codigo.name()); this.codigo = codigo; }
    public Codigo codigo() { return codigo; }
}
