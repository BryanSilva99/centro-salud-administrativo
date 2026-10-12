package pe.centrosalud.unit.billing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.billing.application.ports.PagoRepository;
import pe.centrosalud.modules.billing.application.ports.SesionCajaRepository;
import pe.centrosalud.modules.billing.application.usecases.CerrarCaja;
import pe.centrosalud.modules.billing.domain.EstadoSesionCaja;
import pe.centrosalud.modules.billing.domain.MedioPago;
import pe.centrosalud.modules.billing.domain.Pago;
import pe.centrosalud.modules.billing.domain.SesionCaja;
import pe.centrosalud.modules.billing.domain.TipoMovimiento;

class CerrarCajaTest {

  @Test
  @DisplayName("RF-24/CA-1: cerrar caja sin diferencia")
  void cerrarCajaSinDiferencia() {
    SesionCajaRepository sesiones = mock(SesionCajaRepository.class);
    PagoRepository pagos = mock(PagoRepository.class);
    when(sesiones.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

    UUID sesionId = UUID.randomUUID();
    var sesionAbierta =
        new SesionCaja(
            sesionId,
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            EstadoSesionCaja.ABIERTA,
            Instant.now(),
            null,
            null,
            null,
            null);
    when(sesiones.buscarPorId(sesionId)).thenReturn(Optional.of(sesionAbierta));

    var pago =
        new Pago(
            UUID.randomUUID(),
            UUID.randomUUID(),
            sesionId,
            new BigDecimal("50.00"),
            MedioPago.EFECTIVO,
            null,
            TipoMovimiento.COBRO,
            Instant.now());
    when(pagos.buscarPorSesion(sesionId)).thenReturn(List.of(pago));

    var casoUso = new CerrarCaja(sesiones, pagos);
    var cierre = casoUso.ejecutar(sesionId, new BigDecimal("150.00"), null);

    assertEquals(new BigDecimal("150.00"), cierre.efectivoEsperado());
    assertEquals(new BigDecimal("150.00"), cierre.sesion().efectivoDeclarado());
    assertEquals(0, cierre.sesion().diferencia().compareTo(BigDecimal.ZERO));
  }

  @Test
  @DisplayName("RF-24/CA-2: cerrar caja con diferencia requiere motivo")
  void cerrarCajaConDiferenciaRequiereMotivo() {
    SesionCajaRepository sesiones = mock(SesionCajaRepository.class);
    PagoRepository pagos = mock(PagoRepository.class);

    UUID sesionId = UUID.randomUUID();
    var sesionAbierta =
        new SesionCaja(
            sesionId,
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            EstadoSesionCaja.ABIERTA,
            Instant.now(),
            null,
            null,
            null,
            null);
    when(sesiones.buscarPorId(sesionId)).thenReturn(Optional.of(sesionAbierta));
    when(pagos.buscarPorSesion(sesionId)).thenReturn(List.of());

    var casoUso = new CerrarCaja(sesiones, pagos);

    assertThrows(
        IllegalArgumentException.class,
        () -> casoUso.ejecutar(sesionId, new BigDecimal("120.00"), null));
  }

  @Test
  @DisplayName("RF-24/CA-3: rechazar cierre de sesión ya cerrada")
  void rechazarCierreSesionCerrada() {
    SesionCajaRepository sesiones = mock(SesionCajaRepository.class);
    PagoRepository pagos = mock(PagoRepository.class);

    UUID sesionId = UUID.randomUUID();
    var sesionCerrada =
        new SesionCaja(
            sesionId,
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            EstadoSesionCaja.CERRADA,
            Instant.now(),
            Instant.now(),
            new BigDecimal("150.00"),
            new BigDecimal("0.00"),
            null);
    when(sesiones.buscarPorId(sesionId)).thenReturn(Optional.of(sesionCerrada));

    var casoUso = new CerrarCaja(sesiones, pagos);

    assertThrows(
        IllegalStateException.class,
        () -> casoUso.ejecutar(sesionId, new BigDecimal("150.00"), null));
  }
}
