package pe.centrosalud.modules.appointments.application.usecases;

public final class ReferenciaCitaNoEncontrada extends RuntimeException {
  public ReferenciaCitaNoEncontrada(String referencia) {
    super("La referencia de " + referencia + " no existe o está inactiva");
  }
}
