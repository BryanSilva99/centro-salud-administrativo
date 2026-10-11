package pe.centrosalud.modules.billing.application.usecases;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import pe.centrosalud.modules.billing.application.ports.CargoRepository;
import pe.centrosalud.modules.billing.application.ports.PagoRepository;
import pe.centrosalud.modules.billing.domain.Cargo;
import pe.centrosalud.modules.billing.domain.Pago;

/**
 * Caso de uso: consultar saldos y movimientos.
 * RF-22, RN-13. Muestra cargo, pagos y reversiones.
 */
public final class ConsultarSaldo {
    private final CargoRepository cargos;
    private final PagoRepository pagos;

    public ConsultarSaldo(CargoRepository cargos, PagoRepository pagos) {
        this.cargos = Objects.requireNonNull(cargos);
        this.pagos = Objects.requireNonNull(pagos);
    }

    public SaldoCargo consultarPorCargo(UUID cargoId) {
        Objects.requireNonNull(cargoId, "El cargo es obligatorio");

        var cargo = cargos.buscarPorId(cargoId)
                .orElseThrow(CargoNoEncontrado::new);

        var movimientos = pagos.buscarPorCargo(cargoId);
        var totalPagado = movimientos.stream()
                .filter(Pago::esCobro)
                .map(Pago::importe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalReversiones = movimientos.stream()
                .filter(Pago::esDevolucion)
                .map(Pago::importe)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var saldo = cargo.importe().subtract(totalPagado).add(totalReversiones);

        return new SaldoCargo(cargo, movimientos, totalPagado, totalReversiones, saldo);
    }

    public List<Cargo> consultarPorPaciente(UUID pacienteId) {
        Objects.requireNonNull(pacienteId, "El paciente es obligatorio");
        return cargos.buscarPorPaciente(pacienteId);
    }

    /** Resumen de saldo de un cargo. */
    public record SaldoCargo(
            Cargo cargo,
            List<Pago> movimientos,
            BigDecimal totalPagado,
            BigDecimal totalReversiones,
            BigDecimal saldoPendiente
    ) {}
}
