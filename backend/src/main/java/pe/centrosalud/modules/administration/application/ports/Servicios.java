package pe.centrosalud.modules.administration.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.centrosalud.modules.administration.domain.Servicio;

public interface Servicios {
  List<Servicio> listar();

  Optional<Servicio> buscarPorId(UUID id);

  /**
   * Guarda servicio, modalidades y auditoría en una transacción. Nombre repetido: NOMBRE_DUPLICADO.
   */
  Servicio crear(Servicio nuevo, UUID autor);

  /**
   * Aplica el cambio solo si la versión sigue siendo la de {@code anterior}; si otro cambio la
   * incrementó, lanza CONFLICTO sin escribir nada. Incluye la auditoría en la misma transacción.
   */
  Servicio actualizar(Servicio anterior, Servicio nuevo, UUID autor);
}
