package pe.centrosalud.modules.administration.application.ports;

import java.util.List;
import java.util.UUID;
import pe.centrosalud.modules.administration.domain.TipoRecurso;
import pe.centrosalud.modules.administration.domain.Turno;

public interface Disponibilidad {
  List<Turno> listar(TipoRecurso tipo, UUID recursoId);

  /**
   * Bloquea la fila del recurso, comprueba que el nuevo turno no se superponga con otro activo del
   * mismo recurso y fecha, y lo inserta junto con su auditoría, todo en una transacción.
   * Superpuesto: SUPERPUESTO. Recurso inexistente: RECURSO_INVALIDO.
   */
  Turno crear(TipoRecurso tipo, Turno nuevo, UUID autor);

  /**
   * Idempotente: desactivar un turno ya inactivo no vuelve a auditar. Turno inexistente: NO_EXISTE.
   */
  void desactivar(TipoRecurso tipo, UUID turnoId, UUID autor);
}
