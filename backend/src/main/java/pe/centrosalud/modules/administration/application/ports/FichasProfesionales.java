package pe.centrosalud.modules.administration.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.centrosalud.modules.administration.domain.FichaProfesional;

/** Contrato público usado por identidad para consultar fichas reales, sin conocer sus tablas. */
public interface FichasProfesionales {
  List<FichaProfesional> buscar(String texto);

  Optional<FichaProfesional> buscarPorId(UUID id);

  /** Guarda la ficha, sus servicios habilitados y la auditoría en una transacción. */
  FichaProfesional registrar(FichaProfesional ficha, UUID autor);

  /**
   * Aplica el cambio solo si la versión sigue siendo la de {@code anterior}; si no, CONFLICTO sin
   * escribir nada.
   */
  FichaProfesional actualizar(FichaProfesional anterior, FichaProfesional nuevo, UUID autor);
}
