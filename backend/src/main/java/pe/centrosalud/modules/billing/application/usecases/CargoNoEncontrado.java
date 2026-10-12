package pe.centrosalud.modules.billing.application.usecases;

/** Error de negocio: cargo no encontrado. */
public class CargoNoEncontrado extends RuntimeException {
  public CargoNoEncontrado() {
    super("El cargo no existe");
  }
}
