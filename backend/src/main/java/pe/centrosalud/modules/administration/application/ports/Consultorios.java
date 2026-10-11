package pe.centrosalud.modules.administration.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.centrosalud.modules.administration.domain.Consultorio;

public interface Consultorios {
  List<Consultorio> listar();

  Optional<Consultorio> buscarPorId(UUID id);

  /**
   * Guarda consultorio, relación con servicios y auditoría en una transacción. Código repetido:
   * CODIGO_DUPLICADO.
   */
  Consultorio crear(Consultorio nuevo, UUID autor);

  /**
   * Aplica el cambio solo si la versión sigue siendo la de {@code anterior}; si no, CONFLICTO sin
   * escribir nada.
   */
  Consultorio actualizar(Consultorio anterior, Consultorio nuevo, UUID autor);
}
