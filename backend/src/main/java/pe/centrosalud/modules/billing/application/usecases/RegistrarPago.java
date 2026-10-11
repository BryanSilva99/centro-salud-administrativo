package pe.centrosalud.modules.billing.application.usecases;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.billing.application.ports.CargoRepository;
import pe.centrosalud.modules.billing.application.ports.PagoRepository;
import pe.centrosalud.modules.billing.application.ports.SesionCajaRepository;
import pe.centrosalud.modules.billing.domain.Cargo;
import pe.centrosalud.modules.billing.domain.EstadoCargo;
import pe.centrosalud.modules.billing.domain.MedioPago;
import pe.centrosalud.modules.billing.domain.Pago;
import pe.centrosalud.modules.billing.domain.SesionCaja;
import pe.centrosalud.modules.billing.domain.TipoMovimiento;

/**
 * Caso de uso: registrar pago.
 * RF-21, RN-13, RN-15. Pago total o parcial dentro de una sesión abierta.
 */
public final class RegistrarPago {
    private final PagoRepository pagos;
    private final CargoRepository cargos;
    private final SesionCajaRepository sesiones;

    public RegistrarPago(PagoRepository pagos, CargoRepository cargos, SesionCajaRepository sesiones) {
        this.pagos = Objects.requireNonNull(pagos);
        this.cargos = Objects.requireNonNull(cargos);
        this.sesiones = Objects.requireNonNull(sesiones);
    }

    public Pago ejecutar(UUID cargoId, UUID sesionCajaId, BigDecimal importe, MedioPago medio, String referencia) {
        Objects.requireNonNull(cargoId, "El cargo es obligatorio");
        Objects.requireNonNull(sesionCajaId, "La sesión de caja es obligatoria");
        Objects.requireNonNull(importe, "El importe es obligatorio");
        Objects.requireNonNull(medio, "El medio de pago es obligatorio");

        if (importe.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El importe debe ser positivo");
        }

        var cargo = cargos.buscarPorId(cargoId)
                .orElseThrow(() -> new IllegalArgumentException("El cargo no existe"));
        if (!cargo.estaVigente()) {
            throw new IllegalStateException("El cargo no está vigente");
        }

        var sesion = sesiones.buscarPorId(sesionCajaId)
                .orElseThrow(() -> new IllegalArgumentException("La sesión de caja no existe"));
        if (!sesion.estaAbierta()) {
            throw new IllegalStateException("La sesión de caja no está abierta");
        }

        var saldoPendiente = calcularSaldoPendiente(cargo, importe);
        if (importe.compareTo(saldoPendiente) > 0) {
            throw new IllegalArgumentException("El importe excede el saldo pendiente");
        }

        var pago = new Pago(
                UUID.randomUUID(),
                cargoId,
                sesionCajaId,
                importe,
                medio,
                referencia,
                TipoMovimiento.COBRO,
                Instant.now()
        );

        // Actualizar estado del cargo
        var nuevoEstado = importe.compareTo(saldoPendiente) == 0
                ? EstadoCargo.PAGADO
                : EstadoCargo.PARCIAL;
        var cargoActualizado = new Cargo(
                cargo.id(),
                cargo.citaId(),
                cargo.pacienteId(),
                cargo.importe(),
                nuevoEstado
        );
        cargos.guardar(cargoActualizado);

        return pagos.guardar(pago);
    }

    private BigDecimal calcularSaldoPendiente(Cargo cargo, BigDecimal importePago) {
        var pagosRegistrados = pagos.buscarPorCargo(cargo.id()).stream()
                .filter(Pago::esCobro)
                .map(Pago::importe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return cargo.importe().subtract(pagosRegistrados).subtract(importePago);
    }
}
