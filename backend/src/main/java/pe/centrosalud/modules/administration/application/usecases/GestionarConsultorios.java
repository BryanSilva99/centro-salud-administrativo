package pe.centrosalud.modules.administration.application.usecases;

import static pe.centrosalud.modules.administration.application.usecases.ErrorConsultorios.Codigo.*;

import java.util.Set;
import java.util.UUID;
import pe.centrosalud.modules.administration.application.ports.Consultorios;
import pe.centrosalud.modules.administration.application.ports.Servicios;
import pe.centrosalud.modules.administration.domain.Consultorio;

/** RF-09. La autorización (solo ADM) se exige en la ruta mediante CONSULTORIOS_GESTIONAR. */
public final class GestionarConsultorios {
  private final Consultorios consultorios;
  private final Servicios servicios;

  public GestionarConsultorios(Consultorios consultorios, Servicios servicios) {
    this.consultorios = consultorios;
    this.servicios = servicios;
  }

  public Consultorio crear(
      UUID actor, String codigo, String nombre, Set<UUID> serviciosCompatibles) {
    validarServicios(serviciosCompatibles);
    var nuevo = new Consultorio(UUID.randomUUID(), codigo, nombre, true, 0, serviciosCompatibles);
    return consultorios.crear(nuevo, actor);
  }

  public Consultorio actualizar(
      UUID actor,
      UUID id,
      String codigo,
      String nombre,
      boolean activo,
      Set<UUID> serviciosCompatibles,
      long version) {
    if (version < 0) throw new IllegalArgumentException();
    validarServicios(serviciosCompatibles);
    Consultorio anterior =
        consultorios.buscarPorId(id).orElseThrow(() -> new ErrorConsultorios(NO_EXISTE));
    if (anterior.version() != version) throw new ErrorConsultorios(CONFLICTO);
    var nuevo = new Consultorio(id, codigo, nombre, activo, version, serviciosCompatibles);
    // Un guardado idéntico no incrementa la versión ni genera auditoría artificial.
    if (anterior.equals(nuevo)) return anterior;
    return consultorios.actualizar(anterior, nuevo, actor);
  }

  private void validarServicios(Set<UUID> servicioIds) {
    for (UUID servicioId : servicioIds) {
      if (servicios.buscarPorId(servicioId).isEmpty())
        throw new ErrorConsultorios(SERVICIO_INVALIDO);
    }
  }
}
