package pe.centrosalud.modules.appointments.application.usecases;

public final class CitaNoEncontrada extends RuntimeException {
  public CitaNoEncontrada() {
    super("La cita solicitada no existe");
  }
}
