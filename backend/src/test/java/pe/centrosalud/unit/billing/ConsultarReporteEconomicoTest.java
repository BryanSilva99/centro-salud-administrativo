package pe.centrosalud.unit.billing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import pe.centrosalud.modules.billing.application.ports.PagoRepository;
import pe.centrosalud.modules.billing.application.usecases.ConsultarReporteEconomico;
import pe.centrosalud.modules.billing.domain.MedioPago;
import pe.centrosalud.modules.billing.domain.Pago;
import pe.centrosalud.modules.billing.domain.TipoMovimiento;

class ConsultarReporteEconomicoTest {

    @Test
    @DisplayName("RF-27/CA-1: calcular ingresos netos correctamente")
    void calcularIngresosNetos() {
        PagoRepository pagos = mock(PagoRepository.class);
        when(pagos.buscarPorPeriodo(any(), any())).thenAnswer(inv -> {
            Instant inicio = inv.getArgument(0);
            Instant fin = inv.getArgument(1);
            return List.of(
                new Pago(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"),
                    MedioPago.EFECTIVO, null, TipoMovimiento.COBRO, Instant.now()),
                new Pago(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("20.00"),
                    MedioPago.EFECTIVO, null, TipoMovimiento.DEVOLUCION, Instant.now())
            );
        });

        var casoUso = new ConsultarReporteEconomico(pagos);
        var reporte = casoUso.consultarPorPeriodo(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertEquals(new BigDecimal("100.00"), reporte.totalCobros());
        assertEquals(new BigDecimal("20.00"), reporte.totalDevoluciones());
        assertEquals(new BigDecimal("80.00"), reporte.ingresosNetos());
    }

    @Test
    @DisplayName("RF-27/CA-2: reporte sin movimientos devuelve cero")
    void reporteSinMovimientos() {
        PagoRepository pagos = mock(PagoRepository.class);
        when(pagos.buscarPorPeriodo(any(), any())).thenReturn(List.of());

        var casoUso = new ConsultarReporteEconomico(pagos);
        var reporte = casoUso.consultarPorPeriodo(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertEquals(BigDecimal.ZERO, reporte.totalCobros());
        assertEquals(BigDecimal.ZERO, reporte.totalDevoluciones());
        assertEquals(BigDecimal.ZERO, reporte.ingresosNetos());
    }
}
