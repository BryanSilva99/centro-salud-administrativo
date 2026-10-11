package pe.centrosalud.modules.billing.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Solicitud de anulación económica según RF-23 y RN-14.
 * El solicitante no puede aprobar su propia solicitud.
 */
public record SolicitudReversion(
        UUID id,
        UUID operacionId,
        TipoOperacion operacionTipo,
        UUID solicitanteId,
        String motivo,
        EstadoSolicitudReversion estado,
        UUID aprobadorId,
        Instant fechaSolicitud,
        Instant fechaResolucion
) {
    public SolicitudReversion {
        Objects.requireNonNull(id, "La solicitud debe tener un ID");
        Objects.requireNonNull(operacionId, "La solicitud debe referenciar una operación");
        Objects.requireNonNull(operacionTipo, "La solicitud debe indicar el tipo de operación");
        Objects.requireNonNull(solicitanteId, "La solicitud debe tener un solicitante");
        Objects.requireNonNull(motivo, "La solicitud debe tener un motivo");
        Objects.requireNonNull(estado, "La solicitud debe tener un estado");
        Objects.requireNonNull(fechaSolicitud, "La solicitud debe tener fecha de solicitud");
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo es obligatorio");
        }
    }

    public boolean estaPendiente() {
        return estado == EstadoSolicitudReversion.PENDIENTE;
    }

    public boolean estaAprobada() {
        return estado == EstadoSolicitudReversion.APROBADA;
    }

    public boolean estaRechazada() {
        return estado == EstadoSolicitudReversion.RECHAZADA;
    }

    /** Tipos de operación que pueden revertirse. */
    public enum TipoOperacion {
        CARGO,
        PAGO
    }
}
