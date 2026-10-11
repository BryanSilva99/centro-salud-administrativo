package pe.centrosalud.modules.appointments.application.usecases;

public final class OperacionCitaInvalida extends RuntimeException {
  public OperacionCitaInvalida(String mensaje) {
    super(mensaje);
  }
}
