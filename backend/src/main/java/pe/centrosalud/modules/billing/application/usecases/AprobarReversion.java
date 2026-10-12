package pe.centrosalud.modules.billing.application.usecases;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.billing.application.ports.SolicitudReversionRepository;
import pe.centrosalud.modules.billing.domain.EstadoSolicitudReversion;
import pe.centrosalud.modules.billing.domain.SolicitudReversion;

/**
 * Caso de uso: aprobar o rechazar anulación económica. RF-23, RN-14. El solicitante no puede
 * aprobar su propia solicitud.
 */
public final class AprobarReversion {
  private final SolicitudReversionRepository solicitudes;

  public AprobarReversion(SolicitudReversionRepository solicitudes) {
    this.solicitudes = Objects.requireNonNull(solicitudes);
  }

  public SolicitudReversion ejecutar(UUID solicitudId, UUID aprobadorId, boolean aprobar) {
    Objects.requireNonNull(solicitudId, "La solicitud es obligatoria");
    Objects.requireNonNull(aprobadorId, "El aprobador es obligatorio");

    var solicitud = solicitudes.buscarPorId(solicitudId).orElseThrow(SolicitudNoEncontrada::new);

    if (!solicitud.estaPendiente()) {
      throw new IllegalStateException("La solicitud ya fue resuelta");
    }

    if (solicitud.solicitanteId().equals(aprobadorId)) {
      throw new IllegalArgumentException("El solicitante no puede aprobar su propia solicitud");
    }

    var nuevoEstado =
        aprobar ? EstadoSolicitudReversion.APROBADA : EstadoSolicitudReversion.RECHAZADA;

    var resuelta =
        new SolicitudReversion(
            solicitud.id(),
            solicitud.operacionId(),
            solicitud.operacionTipo(),
            solicitud.solicitanteId(),
            solicitud.motivo(),
            nuevoEstado,
            aprobadorId,
            solicitud.fechaSolicitud(),
            Instant.now());
    return solicitudes.guardar(resuelta);
  }
}
