package pe.centrosalud.modules.billing.application.usecases;

/** Error de negocio: sesión de caja no encontrada. */
public class SesionCajaNoEncontrada extends RuntimeException {
  public SesionCajaNoEncontrada() {
    super("La sesión de caja no existe");
  }
}
