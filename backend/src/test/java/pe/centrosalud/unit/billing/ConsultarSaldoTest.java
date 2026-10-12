package pe.centrosalud.unit.billing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.billing.application.ports.CargoRepository;
import pe.centrosalud.modules.billing.application.ports.PagoRepository;
import pe.centrosalud.modules.billing.application.usecases.ConsultarSaldo;
import pe.centrosalud.modules.billing.domain.Cargo;
import pe.centrosalud.modules.billing.domain.EstadoCargo;
import pe.centrosalud.modules.billing.domain.MedioPago;
import pe.centrosalud.modules.billing.domain.Pago;
import pe.centrosalud.modules.billing.domain.TipoMovimiento;

class ConsultarSaldoTest {

  @Test
  @DisplayName("RF-22/CA-1: consultar saldo con pagos parciales")
  void consultarSaldoConPagosParciales() {
    CargoRepository cargos = mock(CargoRepository.class);
    PagoRepository pagos = mock(PagoRepository.class);

    UUID cargoId = UUID.randomUUID();
    var cargo =
        new Cargo(
            cargoId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            EstadoCargo.PARCIAL);
    when(cargos.buscarPorId(cargoId)).thenReturn(Optional.of(cargo));

    var pago1 =
        new Pago(
            UUID.randomUUID(),
            cargoId,
            UUID.randomUUID(),
            new BigDecimal("30.00"),
            MedioPago.EFECTIVO,
            null,
            TipoMovimiento.COBRO,
            Instant.now());
    var pago2 =
        new Pago(
            UUID.randomUUID(),
            cargoId,
            UUID.randomUUID(),
            new BigDecimal("20.00"),
            MedioPago.TARJETA,
            "REF123",
            TipoMovimiento.COBRO,
            Instant.now());
    when(pagos.buscarPorCargo(cargoId)).thenReturn(List.of(pago1, pago2));

    var casoUso = new ConsultarSaldo(cargos, pagos);
    var saldo = casoUso.consultarPorCargo(cargoId);

    assertEquals(new BigDecimal("50.00"), saldo.saldoPendiente());
    assertEquals(new BigDecimal("50.00"), saldo.totalPagado());
    assertEquals(BigDecimal.ZERO, saldo.totalReversiones());
  }

  @Test
  @DisplayName("RF-22: consultar saldo de cargo no encontrado lanza excepción")
  void consultarSaldoCargoNoExiste() {
    CargoRepository cargos = mock(CargoRepository.class);
    PagoRepository pagos = mock(PagoRepository.class);

    when(cargos.buscarPorId(any())).thenReturn(Optional.empty());

    var casoUso = new ConsultarSaldo(cargos, pagos);

    assertThrows(
        pe.centrosalud.modules.billing.application.usecases.CargoNoEncontrado.class,
        () -> casoUso.consultarPorCargo(UUID.randomUUID()));
  }
}
