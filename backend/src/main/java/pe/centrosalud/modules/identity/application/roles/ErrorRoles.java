package pe.centrosalud.modules.identity.application.roles;
public final class ErrorRoles extends RuntimeException {
    public enum Codigo { NO_EXISTE, PROHIBIDO, CONFLICTO, ULTIMO_ADMIN, DUPLICADO, PROFESIONAL_INVALIDO, VINCULO_OCUPADO, REQUIERE_PRO }
    private final Codigo codigo;
    public ErrorRoles(Codigo codigo) { super(codigo.name()); this.codigo = codigo; }
    public Codigo codigo() { return codigo; }
}
