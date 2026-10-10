package pe.centrosalud.modules.identity.application.usecases;

public final class CredencialesInvalidas extends RuntimeException {
    public CredencialesInvalidas() {
        super("Identificador o contraseña incorrectos");
    }
}
