package pe.centrosalud.modules.administration.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.centrosalud.modules.administration.domain.FichaProfesional;

/** Contrato público usado por identidad para consultar fichas reales, sin conocer sus tablas. */
public interface FichasProfesionales {
  List<FichaProfesional> buscar(String texto);

  Optional<FichaProfesional> buscarPorId(UUID id);

  FichaProfesional registrar(FichaProfesional ficha, UUID autor);
}
