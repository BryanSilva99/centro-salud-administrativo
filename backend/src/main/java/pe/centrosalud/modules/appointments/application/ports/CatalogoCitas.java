package pe.centrosalud.modules.appointments.application.ports;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Referencias mínimas que appointments necesita de otros módulos. */
public interface CatalogoCitas {
  Optional<Paciente> paciente(UUID id);

  Optional<Profesional> profesional(UUID id);

  Optional<Servicio> servicio(UUID id);

  Optional<Consultorio> consultorio(UUID id);

  Contexto contexto();

  default Contexto contexto(boolean incluirPacientes, UUID profesionalId) {
    var todos = contexto();
    return new Contexto(
        incluirPacientes ? todos.pacientes() : List.of(),
        todos.profesionales().stream()
            .filter(p -> profesionalId == null || p.id().equals(profesionalId))
            .toList(),
        todos.servicios(),
        todos.consultorios());
  }

  record Paciente(UUID id, String nombre, boolean activo) {}

  record Profesional(UUID id, String nombre, String especialidad, boolean activo) {}

  record Consultorio(UUID id, String nombre, boolean activo) {}

  record Servicio(
      UUID id, String nombre, boolean activo, int duracionMinutos, List<Modalidad> modalidades) {
    public Servicio {
      modalidades = List.copyOf(modalidades);
    }

    public Optional<Modalidad> modalidad(String codigo) {
      return modalidades.stream().filter(item -> item.codigo().equals(codigo)).findFirst();
    }
  }

  record Modalidad(String codigo, String nombre, BigDecimal importe) {}

  record Contexto(
      List<Paciente> pacientes,
      List<Profesional> profesionales,
      List<Servicio> servicios,
      List<Consultorio> consultorios) {
    public Contexto {
      pacientes = List.copyOf(pacientes);
      profesionales = List.copyOf(profesionales);
      servicios = List.copyOf(servicios);
      consultorios = List.copyOf(consultorios);
    }
  }
}
