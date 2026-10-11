package pe.centrosalud.unit.billing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import pe.centrosalud.modules.billing.application.ports.CargoRepository;
import pe.centrosalud.modules.billing.application.ports.PagoRepository;
import pe.centrosalud.modules.billing.application.ports.SesionCajaRepository;
import pe.centrosalud.modules.billing.application.usecases.RegistrarPago;
import pe.centrosalud.modules.billing.domain.Cargo;
import pe.centrosalud.modules.billing.domain.EstadoCargo;
import pe.centrosalud.modules.billing.domain.EstadoSesionCaja;
import pe.centrosalud.modules.billing.domain.MedioPago;
import pe.centrosalud.modules.billing.domain.SesionCaja;
import pe.centrosalud.modules.billing.domain.TipoMovimiento;

class RegistrarPagoTest {

    @Test
    @DisplayName("RF-21/CA-1: registrar pago total válido")
    void registrarPagoTotalValido() {
        PagoRepository pagos = mock(PagoRepository.class);
        CargoRepository cargos = mock(CargoRepository.class);
        SesionCajaRepository sesiones = mock(SesionCajaRepository.class);

        UUID cargoId = UUID.randomUUID();
        var cargo = new Cargo(cargoId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), EstadoCargo.PENDIENTE);
        when(cargos.buscarPorId(cargoId)).thenReturn(Optional.of(cargo));
        when(cargos.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID sesionId = UUID.randomUUID();
        var sesion = new SesionCaja(sesionId, UUID.randomUUID(), new BigDecimal("50.00"), EstadoSesionCaja.ABIERTA,
                Instant.now(), null, null, null, null);
        when(sesiones.buscarPorId(sesionId)).thenReturn(Optional.of(sesion));

        when(pagos.buscarPorCargo(cargoId)).thenReturn(List.of());
        when(pagos.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        var casoUso = new RegistrarPago(pagos, cargos, sesiones);
        var pago = casoUso.ejecutar(cargoId, sesionId, new BigDecimal("100.00"), MedioPago.EFECTIVO, null);

        assertNotNull(pago);
        assertEquals(new BigDecimal("100.00"), pago.importe());
        assertEquals(MedioPago.EFECTIVO, pago.medio());
        assertEquals(TipoMovimiento.COBRO, pago.tipo());
    }

    @Test
    @DisplayName("RF-21/CA-2: rechazar pago que excede saldo pendiente")
    void rechazarPagoExcedeSaldo() {
        PagoRepository pagos = mock(PagoRepository.class);
        CargoRepository cargos = mock(CargoRepository.class);
        SesionCajaRepository sesiones = mock(SesionCajaRepository.class);

        UUID cargoId = UUID.randomUUID();
        var cargo = new Cargo(cargoId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"), EstadoCargo.PENDIENTE);
        when(cargos.buscarPorId(cargoId)).thenReturn(Optional.of(cargo));

        UUID sesionId = UUID.randomUUID();
        var sesion = new SesionCaja(sesionId, UUID.randomUUID(), new BigDecimal("0.00"), EstadoSesionCaja.ABIERTA,
                Instant.now(), null, null, null, null);
        when(sesiones.buscarPorId(sesionId)).thenReturn(Optional.of(sesion));

        when(pagos.buscarPorCargo(cargoId)).thenReturn(List.of());

        var casoUso = new RegistrarPago(pagos, cargos, sesiones);

        assertThrows(IllegalArgumentException.class, () ->
            casoUso.ejecutar(cargoId, sesionId, new BigDecimal("60.00"), MedioPago.EFECTIVO, null)
        );
    }

    @Test
    @DisplayName("RF-21: rechazar pago con sesión cerrada")
    void rechazarPagoSesionCerrada() {
        PagoRepository pagos = mock(PagoRepository.class);
        CargoRepository cargos = mock(CargoRepository.class);
        SesionCajaRepository sesiones = mock(SesionCajaRepository.class);

        UUID cargoId = UUID.randomUUID();
        var cargo = new Cargo(cargoId, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("50.00"), EstadoCargo.PENDIENTE);
        when(cargos.buscarPorId(cargoId)).thenReturn(Optional.of(cargo));

        UUID sesionId = UUID.randomUUID();
        var sesion = new SesionCaja(sesionId, UUID.randomUUID(), new BigDecimal("0.00"), EstadoSesionCaja.CERRADA,
                Instant.now(), Instant.now(), new BigDecimal("0.00"), new BigDecimal("0.00"), null);
        when(sesiones.buscarPorId(sesionId)).thenReturn(Optional.of(sesion));

        var casoUso = new RegistrarPago(pagos, cargos, sesiones);

        assertThrows(IllegalStateException.class, () ->
            casoUso.ejecutar(cargoId, sesionId, new BigDecimal("30.00"), MedioPago.EFECTIVO, null)
        );
    }
}
