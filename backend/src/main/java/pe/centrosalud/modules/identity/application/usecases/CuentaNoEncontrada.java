package pe.centrosalud.modules.identity.application.usecases;

public class CuentaNoEncontrada extends RuntimeException {
    public CuentaNoEncontrada() {
        super("No se encontró la cuenta solicitada");
    }
}
