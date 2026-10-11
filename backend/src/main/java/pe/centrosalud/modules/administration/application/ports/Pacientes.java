package pe.centrosalud.modules.administration.application.ports;

import java.util.UUID;
import pe.centrosalud.modules.administration.domain.Paciente;

public interface Pacientes {
  /**
   * Guarda el paciente y su auditoría en una sola transacción y devuelve el código asignado. Lanza
   * PacienteDuplicado si ya existe ese tipo y número de documento.
   */
  Paciente registrar(Paciente nuevo, UUID autor);
}
