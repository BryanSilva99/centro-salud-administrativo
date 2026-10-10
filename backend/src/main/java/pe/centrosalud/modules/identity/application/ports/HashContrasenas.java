package pe.centrosalud.modules.identity.application.ports;

public interface HashContrasenas {
    String generar(String contrasena);

    /** Un hash ausente o inválido debe rechazarse sin omitir el trabajo criptográfico. */
    boolean coincide(String contrasena, String hash);
}
