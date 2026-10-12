package pe.centrosalud.modules.billing.application.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.centrosalud.modules.billing.domain.SolicitudReversion;

/** Contrato de persistencia para solicitudes de reversión; implementado por PostgreSQL. */
public interface SolicitudReversionRepository {
  List<SolicitudReversion> listar();

  SolicitudReversion guardar(SolicitudReversion solicitud);

  Optional<SolicitudReversion> buscarPorId(UUID id);

  List<SolicitudReversion> buscarPorSolicitante(UUID solicitanteId);
}
