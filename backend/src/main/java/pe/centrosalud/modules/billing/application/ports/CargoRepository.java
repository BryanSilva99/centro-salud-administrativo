package pe.centrosalud.modules.billing.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.centrosalud.modules.billing.domain.Cargo;

/** Contrato de persistencia para cargos; implementado por PostgreSQL. */
public interface CargoRepository {
  Cargo guardar(Cargo cargo);

  Optional<Cargo> buscarPorId(UUID id);

  List<Cargo> buscarPorCita(UUID citaId);

  List<Cargo> buscarPorPaciente(UUID pacienteId);
}
