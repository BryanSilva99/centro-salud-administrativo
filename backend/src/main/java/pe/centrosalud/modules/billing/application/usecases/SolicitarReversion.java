package pe.centrosalud.modules.billing.application.usecases;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.billing.application.ports.SolicitudReversionRepository;
import pe.centrosalud.modules.billing.domain.EstadoSolicitudReversion;
import pe.centrosalud.modules.billing.domain.SolicitudReversion;

/**
 * Caso de uso: solicitar anulación económica. RF-23, RN-14. El solicitante no puede aprobar su
 * propia solicitud.
 */
public final class SolicitarReversion {
  private final SolicitudReversionRepository solicitudes;

  public SolicitarReversion(SolicitudReversionRepository solicitudes) {
    this.solicitudes = Objects.requireNonNull(solicitudes);
  }

  public SolicitudReversion ejecutar(
      UUID operacionId, SolicitudReversion.TipoOperacion tipo, UUID solicitanteId, String motivo) {
    Objects.requireNonNull(operacionId, "La operación es obligatoria");
    Objects.requireNonNull(tipo, "El tipo de operación es obligatorio");
    Objects.requireNonNull(solicitanteId, "El solicitante es obligatorio");
    Objects.requireNonNull(motivo, "El motivo es obligatorio");

    if (motivo.isBlank()) {
      throw new IllegalArgumentException("El motivo es obligatorio");
    }

    var solicitud =
        new SolicitudReversion(
            UUID.randomUUID(),
            operacionId,
            tipo,
            solicitanteId,
            motivo,
            EstadoSolicitudReversion.PENDIENTE,
            null,
            Instant.now(),
            null);
    return solicitudes.guardar(solicitud);
  }
}
