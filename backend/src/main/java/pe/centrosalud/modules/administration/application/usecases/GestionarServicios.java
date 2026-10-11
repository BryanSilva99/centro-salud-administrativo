package pe.centrosalud.modules.administration.application.usecases;

import static pe.centrosalud.modules.administration.application.usecases.ErrorServicios.Codigo.*;

import java.math.BigDecimal;
import java.util.*;
import pe.centrosalud.modules.administration.application.ports.Servicios;
import pe.centrosalud.modules.administration.domain.Modalidad;
import pe.centrosalud.modules.administration.domain.Servicio;

/**
 * RF-08. La autorización (solo ADM) se exige en la ruta mediante SERVICIOS_GESTIONAR. Las
 * modalidades nunca se eliminan: para retirarlas se envían con activa=false.
 */
public final class GestionarServicios {
  /** Modalidad solicitada; id null indica una modalidad nueva. */
  public record DatosModalidad(
      UUID id, String nombre, boolean conCargo, BigDecimal tarifa, boolean activa) {}

  private final Servicios servicios;

  public GestionarServicios(Servicios servicios) {
    this.servicios = servicios;
  }

  public Servicio crear(
      UUID actor, String nombre, int duracionMinutos, List<DatosModalidad> modalidades) {
    if (modalidades.stream().anyMatch(m -> m.id() != null))
      throw new ErrorServicios(MODALIDAD_INVALIDA);
    var nuevo =
        new Servicio(UUID.randomUUID(), nombre, duracionMinutos, true, 0, modalidades(modalidades));
    return servicios.crear(nuevo, actor);
  }

  public Servicio actualizar(
      UUID actor,
      UUID id,
      String nombre,
      int duracionMinutos,
      boolean activo,
      List<DatosModalidad> modalidades,
      long version) {
    if (version < 0) throw new IllegalArgumentException();
    Servicio anterior = servicios.buscarPorId(id).orElseThrow(() -> new ErrorServicios(NO_EXISTE));
    if (anterior.version() != version) throw new ErrorServicios(CONFLICTO);
    validarIdentificadores(anterior, modalidades);
    var nuevo =
        new Servicio(id, nombre, duracionMinutos, activo, version, modalidades(modalidades));
    // Un guardado idéntico no incrementa la versión ni genera auditoría artificial.
    if (mismoContenido(anterior, nuevo)) return anterior;
    return servicios.actualizar(anterior, nuevo, actor);
  }

  /**
   * Cada id debe pertenecer al servicio, sin repetirse, y ninguna modalidad existente puede
   * omitirse.
   */
  private static void validarIdentificadores(Servicio anterior, List<DatosModalidad> modalidades) {
    var existentes = new HashSet<UUID>();
    for (Modalidad modalidad : anterior.modalidades()) existentes.add(modalidad.id());
    var recibidos = new HashSet<UUID>();
    for (DatosModalidad modalidad : modalidades) {
      if (modalidad.id() == null) continue;
      if (!existentes.contains(modalidad.id()) || !recibidos.add(modalidad.id()))
        throw new ErrorServicios(MODALIDAD_INVALIDA);
    }
    if (!recibidos.equals(existentes)) throw new ErrorServicios(MODALIDAD_INVALIDA);
  }

  private static List<Modalidad> modalidades(List<DatosModalidad> datos) {
    return datos.stream()
        .map(
            m ->
                new Modalidad(
                    m.id() == null ? UUID.randomUUID() : m.id(),
                    m.nombre(),
                    m.conCargo(),
                    m.tarifa(),
                    m.activa()))
        .toList();
  }

  private static boolean mismoContenido(Servicio a, Servicio b) {
    return a.nombre().equals(b.nombre())
        && a.duracionMinutos() == b.duracionMinutos()
        && a.activo() == b.activo()
        && Set.copyOf(a.modalidades()).equals(Set.copyOf(b.modalidades()));
  }
}
