package pe.centrosalud.adapters.outbound.modules.appointments;

import java.util.*;
import pe.centrosalud.modules.administration.application.ports.*;
import pe.centrosalud.modules.appointments.application.ports.CatalogoCitas;

/** Traduce los contratos administrativos sin acceder a sus tablas. */
public final class CatalogoCitasAdministracion implements CatalogoCitas {
  private final Pacientes pacientes;
  private final FichasProfesionales profesionales;
  private final Servicios servicios;
  private final Consultorios consultorios;

  public CatalogoCitasAdministracion(
      Pacientes p, FichasProfesionales f, Servicios s, Consultorios c) {
    pacientes = p;
    profesionales = f;
    servicios = s;
    consultorios = c;
  }

  public Optional<Paciente> paciente(UUID id) {
    return pacientes
        .buscarPorId(id)
        .map(p -> new Paciente(p.id(), p.nombres() + " " + p.apellidos(), true));
  }

  public Optional<Profesional> profesional(UUID id) {
    return profesionales
        .buscarPorId(id)
        .map(p -> new Profesional(p.id(), p.nombre(), p.funcion(), p.activa()));
  }

  public Optional<Servicio> servicio(UUID id) {
    return servicios.buscarPorId(id).map(this::servicio);
  }

  private Servicio servicio(pe.centrosalud.modules.administration.domain.Servicio s) {
    return new Servicio(
        s.id(),
        s.nombre(),
        s.activo(),
        s.duracionMinutos(),
        s.modalidades().stream()
            .filter(m -> m.activa())
            .map(m -> new Modalidad(m.id().toString(), m.nombre(), m.tarifa()))
            .toList());
  }

  public Optional<Consultorio> consultorio(UUID id) {
    return consultorios.buscarPorId(id).map(c -> new Consultorio(c.id(), c.nombre(), c.activo()));
  }

  public Contexto contexto() {
    return contexto(true, null);
  }

  @Override
  public Contexto contexto(boolean incluirPacientes, UUID profesionalId) {
    var lista = new ArrayList<Paciente>();
    if (incluirPacientes) {
      for (int n = 0; ; n++) {
        var pagina = pacientes.buscar("", n);
        pagina
            .elementos()
            .forEach(p -> lista.add(new Paciente(p.id(), p.nombres() + " " + p.apellidos(), true)));
        if (!pagina.hayMas()) break;
      }
    }
    var fichas =
        profesionalId == null
            ? profesionales.listar()
            : profesionales.buscarPorId(profesionalId).stream().toList();
    return new Contexto(
        lista,
        fichas.stream()
            .map(p -> new Profesional(p.id(), p.nombre(), p.funcion(), p.activa()))
            .toList(),
        servicios.listar().stream().map(this::servicio).toList(),
        consultorios.listar().stream()
            .map(c -> new Consultorio(c.id(), c.nombre(), c.activo()))
            .toList());
  }
}
