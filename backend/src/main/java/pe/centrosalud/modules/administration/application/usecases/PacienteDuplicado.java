package pe.centrosalud.modules.administration.application.usecases;

/** No incluye el documento: el mensaje puede terminar en respuestas o registros. */
public class PacienteDuplicado extends RuntimeException {
  public PacienteDuplicado() {
    super("Ya existe un paciente con ese documento");
  }
}
