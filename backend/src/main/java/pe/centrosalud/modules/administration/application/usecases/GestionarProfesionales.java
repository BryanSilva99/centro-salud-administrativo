package pe.centrosalud.modules.administration.application.usecases;

import static pe.centrosalud.modules.administration.application.usecases.ErrorProfesionales.Codigo.*;

import java.util.Set;
import java.util.UUID;
import pe.centrosalud.modules.administration.application.ports.FichasProfesionales;
import pe.centrosalud.modules.administration.application.ports.Servicios;
import pe.centrosalud.modules.administration.domain.FichaProfesional;

/**
 * RF-07. La autorización (solo ADM) se exige en la ruta mediante PROFESIONALES_GESTIONAR. El
 * identificador duplicado lo resuelve la restricción única de PostgreSQL (DuplicateKeyException),
 * igual que en el alta existente de RF-02.
 */
public final class GestionarProfesionales {
  private final FichasProfesionales fichas;
  private final Servicios servicios;

  public GestionarProfesionales(FichasProfesionales fichas, Servicios servicios) {
    this.fichas = fichas;
    this.servicios = servicios;
  }

  public FichaProfesional crear(
      UUID actor,
      String identificacion,
      String nombre,
      String funcion,
      Set<UUID> serviciosHabilitados) {
    Set<UUID> servicios = serviciosHabilitados == null ? Set.of() : serviciosHabilitados;
    validarServicios(servicios);
    var nueva =
        new FichaProfesional(
            UUID.randomUUID(), identificacion, nombre, funcion, true, 0, servicios);
    return fichas.registrar(nueva, actor);
  }

  public FichaProfesional actualizar(
      UUID actor,
      UUID id,
      String identificacion,
      String nombre,
      String funcion,
      boolean activa,
      Set<UUID> serviciosHabilitados,
      long version) {
    if (version < 0) throw new IllegalArgumentException();
    validarServicios(serviciosHabilitados);
    FichaProfesional anterior =
        fichas.buscarPorId(id).orElseThrow(() -> new ErrorProfesionales(NO_EXISTE));
    if (anterior.version() != version) throw new ErrorProfesionales(CONFLICTO);
    var nueva =
        new FichaProfesional(
            id, identificacion, nombre, funcion, activa, version, serviciosHabilitados);
    // Un guardado idéntico no incrementa la versión ni genera auditoría artificial.
    if (anterior.equals(nueva)) return anterior;
    return fichas.actualizar(anterior, nueva, actor);
  }

  private void validarServicios(Set<UUID> servicioIds) {
    for (UUID servicioId : servicioIds) {
      if (servicios.buscarPorId(servicioId).isEmpty())
        throw new ErrorProfesionales(SERVICIO_INVALIDO);
    }
  }
}
