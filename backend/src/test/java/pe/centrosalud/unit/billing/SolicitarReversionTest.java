package pe.centrosalud.unit.billing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.billing.application.ports.SolicitudReversionRepository;
import pe.centrosalud.modules.billing.application.usecases.SolicitarReversion;
import pe.centrosalud.modules.billing.domain.EstadoSolicitudReversion;
import pe.centrosalud.modules.billing.domain.SolicitudReversion;

class SolicitarReversionTest {

  @Test
  @DisplayName("RF-23/CA-1: solicitar reversión con motivo válido")
  void solicitarReversionValida() {
    SolicitudReversionRepository solicitudes = mock(SolicitudReversionRepository.class);
    when(solicitudes.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

    var casoUso = new SolicitarReversion(solicitudes);
    UUID operacionId = UUID.randomUUID();
    UUID solicitanteId = UUID.randomUUID();

    var solicitud =
        casoUso.ejecutar(
            operacionId, SolicitudReversion.TipoOperacion.PAGO, solicitanteId, "Pago duplicado");

    assertNotNull(solicitud);
    assertEquals(operacionId, solicitud.operacionId());
    assertEquals(SolicitudReversion.TipoOperacion.PAGO, solicitud.operacionTipo());
    assertEquals(solicitanteId, solicitud.solicitanteId());
    assertEquals(EstadoSolicitudReversion.PENDIENTE, solicitud.estado());
    assertEquals("Pago duplicado", solicitud.motivo());
  }

  @Test
  @DisplayName("RF-23: rechazar solicitud sin motivo")
  void rechazarSinMotivo() {
    SolicitudReversionRepository solicitudes = mock(SolicitudReversionRepository.class);
    var casoUso = new SolicitarReversion(solicitudes);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            casoUso.ejecutar(
                UUID.randomUUID(), SolicitudReversion.TipoOperacion.CARGO, UUID.randomUUID(), ""));
  }
}
