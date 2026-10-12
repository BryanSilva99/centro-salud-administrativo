package pe.centrosalud.modules.billing.application.ports;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import pe.centrosalud.modules.billing.domain.Pago;

/** Contrato de persistencia para pagos; implementado por PostgreSQL. */
public interface PagoRepository {
  java.util.Optional<Pago> buscarPorId(UUID id);

  Pago guardar(Pago pago);

  List<Pago> buscarPorCargo(UUID cargoId);

  List<Pago> buscarPorSesion(UUID sesionCajaId);

  List<Pago> buscarPorPeriodo(Instant inicio, Instant fin);
}
