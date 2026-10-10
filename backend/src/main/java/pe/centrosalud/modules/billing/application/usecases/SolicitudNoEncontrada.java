package pe.centrosalud.modules.billing.application.usecases;

/** Error de negocio: solicitud de reversión no encontrada. */
public class SolicitudNoEncontrada extends RuntimeException {
    public SolicitudNoEncontrada() {
        super("La solicitud de reversión no existe");
    }
}
