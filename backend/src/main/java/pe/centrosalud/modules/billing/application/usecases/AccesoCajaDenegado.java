package pe.centrosalud.modules.billing.application.usecases;

public class AccesoCajaDenegado extends RuntimeException {
  public AccesoCajaDenegado() {
    super("No tienes permiso para esta operación de caja");
  }
}
