package pe.centrosalud.modules.appointments.application.usecases;

public final class AccesoCitasDenegado extends RuntimeException {
  public AccesoCitasDenegado() {
    super("La cuenta no tiene permisos para operar citas");
  }
}
