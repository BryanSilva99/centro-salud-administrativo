package pe.centrosalud.adapters.outbound.modules.billing;

import java.time.LocalDate;
import java.util.*;
import pe.centrosalud.modules.administration.application.ports.Pacientes;
import pe.centrosalud.modules.appointments.application.ports.Citas;
import pe.centrosalud.modules.appointments.domain.Cita;
import pe.centrosalud.modules.billing.application.ports.ReservasCaja;

public record ReservasCajaCitas(Citas citas, Pacientes pacientes) implements ReservasCaja {
  public Reserva consultar(UUID id) {
    return convertir(
        citas
            .buscarParaCargo(id)
            .orElseThrow(() -> new IllegalArgumentException("La cita no existe")));
  }

  public List<Reserva> listar(LocalDate fecha) {
    return citas.agenda(fecha, null, null, null).stream().map(this::convertir).toList();
  }

  private Reserva convertir(Cita c) {
    var p =
        pacientes
            .buscarPorId(c.pacienteId())
            .orElseThrow(() -> new IllegalArgumentException("El paciente no existe"));
    return new Reserva(
        c.id(),
        c.pacienteId(),
        p.nombres() + " " + p.apellidos(),
        c.horario().inicio(),
        c.estado().name(),
        c.modalidad(),
        c.importe());
  }
}
