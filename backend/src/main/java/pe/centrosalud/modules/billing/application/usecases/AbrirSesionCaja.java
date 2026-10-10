package pe.centrosalud.modules.billing.application.usecases;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.billing.application.ports.SesionCajaRepository;
import pe.centrosalud.modules.billing.domain.EstadoSesionCaja;
import pe.centrosalud.modules.billing.domain.SesionCaja;

/**
 * Caso de uso: abrir sesión de caja.
 * RF-20, RN-15. Una sola sesión abierta por cajero.
 */
public final class AbrirSesionCaja {
    private final SesionCajaRepository sesiones;

    public AbrirSesionCaja(SesionCajaRepository sesiones) {
        this.sesiones = Objects.requireNonNull(sesiones);
    }

    public SesionCaja ejecutar(UUID cajeroId, BigDecimal fondoInicial) {
        Objects.requireNonNull(cajeroId, "El cajero es obligatorio");
        Objects.requireNonNull(fondoInicial, "El fondo inicial es obligatorio");

        if (fondoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El fondo inicial no puede ser negativo");
        }

        var sesionAbierta = sesiones.buscarSesionAbiertaPorCajero(cajeroId);
        if (sesionAbierta.isPresent()) {
            throw new IllegalStateException("El cajero ya tiene una sesión de caja abierta");
        }

        var sesion = new SesionCaja(
                UUID.randomUUID(),
                cajeroId,
                fondoInicial,
                EstadoSesionCaja.ABIERTA,
                Instant.now(),
                null,
                null,
                null,
                null
        );
        return sesiones.guardar(sesion);
    }
}
