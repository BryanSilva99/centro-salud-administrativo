package pe.centrosalud.modules.billing.application.ports;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public interface ReservasCaja {
  Reserva consultar(UUID citaId);

  List<Reserva> listar(LocalDate fecha);

  record Reserva(
      UUID id,
      UUID pacienteId,
      String paciente,
      Instant inicio,
      String estado,
      String modalidad,
      BigDecimal importe) {}
}
