package pe.centrosalud.modules.administration.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.centrosalud.modules.administration.domain.Paciente;

public interface Pacientes {
  /** Guarda el paciente y su auditoría en una sola transacción y devuelve el código asignado. */
  Paciente registrar(Paciente nuevo, UUID autor);

  Optional<Paciente> buscarPorId(UUID id);

  /** Busca por código, documento o nombre; 20 elementos por página. */
  Pagina<Paciente> buscar(String texto, int pagina);

  /**
   * Aplica el cambio solo si la versión sigue siendo la de {@code anterior}; si no, CONFLICTO sin
   * escribir nada.
   */
  Paciente actualizar(Paciente anterior, Paciente nuevo, UUID autor);

  record Pagina<T>(List<T> elementos, boolean hayMas) {}
}
