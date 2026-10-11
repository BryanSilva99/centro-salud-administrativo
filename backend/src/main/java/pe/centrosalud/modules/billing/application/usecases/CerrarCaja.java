package pe.centrosalud.modules.billing.application.usecases;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.billing.application.ports.PagoRepository;
import pe.centrosalud.modules.billing.application.ports.SesionCajaRepository;
import pe.centrosalud.modules.billing.domain.EstadoSesionCaja;
import pe.centrosalud.modules.billing.domain.MedioPago;
import pe.centrosalud.modules.billing.domain.Pago;
import pe.centrosalud.modules.billing.domain.SesionCaja;

/**
 * Caso de uso: cerrar caja y conciliar.
 * RF-24, RN-15. Calcula efectivo esperado, diferencia y congela el resumen.
 */
public final class CerrarCaja {
    private final SesionCajaRepository sesiones;
    private final PagoRepository pagos;

    public CerrarCaja(SesionCajaRepository sesiones, PagoRepository pagos) {
        this.sesiones = Objects.requireNonNull(sesiones);
        this.pagos = Objects.requireNonNull(pagos);
    }

    public CierreCaja ejecutar(UUID sesionId, BigDecimal efectivoDeclarado, String motivoDiferencia) {
        Objects.requireNonNull(sesionId, "La sesión es obligatoria");
        Objects.requireNonNull(efectivoDeclarado, "El efectivo declarado es obligatoria");

        var sesion = sesiones.buscarPorId(sesionId)
                .orElseThrow(SesionCajaNoEncontrada::new);

        if (!sesion.estaAbierta()) {
            throw new IllegalStateException("La sesión de caja ya está cerrada");
        }

        var movimientos = pagos.buscarPorSesion(sesionId);
        var cobrosEfectivo = movimientos.stream()
                .filter(p -> p.esCobro() && p.medio() == MedioPago.EFECTIVO)
                .map(Pago::importe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var devolucionesEfectivo = movimientos.stream()
                .filter(p -> p.esDevolucion() && p.medio() == MedioPago.EFECTIVO)
                .map(Pago::importe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var efectivoEsperado = sesion.fondoInicial().add(cobrosEfectivo).subtract(devolucionesEfectivo);
        var diferencia = efectivoDeclarado.subtract(efectivoEsperado);

        if (diferencia.compareTo(BigDecimal.ZERO) != 0 && (motivoDiferencia == null || motivoDiferencia.isBlank())) {
            throw new IllegalArgumentException("Se requiere motivo cuando la diferencia es distinta de cero");
        }

        var cerrada = new SesionCaja(
                sesion.id(),
                sesion.cajeroId(),
                sesion.fondoInicial(),
                EstadoSesionCaja.CERRADA,
                sesion.fechaApertura(),
                Instant.now(),
                efectivoDeclarado,
                diferencia,
                motivoDiferencia
        );
        sesiones.guardar(cerrada);

        return new CierreCaja(cerrada, movimientos, efectivoEsperado, cobrosEfectivo, devolucionesEfectivo);
    }

    /** Resumen del cierre de caja. */
    public record CierreCaja(
            SesionCaja sesion,
            java.util.List<Pago> movimientos,
            BigDecimal efectivoEsperado,
            BigDecimal cobrosEfectivo,
            BigDecimal devolucionesEfectivo
    ) {}
}
