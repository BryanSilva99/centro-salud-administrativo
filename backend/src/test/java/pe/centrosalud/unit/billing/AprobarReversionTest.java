package pe.centrosalud.unit.billing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import pe.centrosalud.modules.billing.application.ports.SolicitudReversionRepository;
import pe.centrosalud.modules.billing.application.usecases.AprobarReversion;
import pe.centrosalud.modules.billing.domain.EstadoSolicitudReversion;
import pe.centrosalud.modules.billing.domain.SolicitudReversion;

class AprobarReversionTest {

    @Test
    @DisplayName("RF-23/CA-2: aprobar reversión con usuario distinto al solicitante")
    void aprobarReversionValida() {
        SolicitudReversionRepository solicitudes = mock(SolicitudReversionRepository.class);
        when(solicitudes.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID solicitudId = UUID.randomUUID();
        UUID solicitanteId = UUID.randomUUID();
        UUID aprobadorId = UUID.randomUUID();

        var solicitudPendiente = new SolicitudReversion(
                solicitudId, UUID.randomUUID(), SolicitudReversion.TipoOperacion.PAGO,
                solicitanteId, "Motivo", EstadoSolicitudReversion.PENDIENTE, null, Instant.now(), null);
        when(solicitudes.buscarPorId(solicitudId)).thenReturn(Optional.of(solicitudPendiente));

        var casoUso = new AprobarReversion(solicitudes);
        var resuelta = casoUso.ejecutar(solicitudId, aprobadorId, true);

        assertEquals(EstadoSolicitudReversion.APROBADA, resuelta.estado());
        assertEquals(aprobadorId, resuelta.aprobadorId());
    }

    @Test
    @DisplayName("RF-23/CA-2: rechazar aprobación cuando solicitante = aprobador")
    void rechazarApropiaSolicitud() {
        SolicitudReversionRepository solicitudes = mock(SolicitudReversionRepository.class);

        UUID solicitudId = UUID.randomUUID();
        UUID mismoUsuario = UUID.randomUUID();

        var solicitudPendiente = new SolicitudReversion(
                solicitudId, UUID.randomUUID(), SolicitudReversion.TipoOperacion.PAGO,
                mismoUsuario, "Motivo", EstadoSolicitudReversion.PENDIENTE, null, Instant.now(), null);
        when(solicitudes.buscarPorId(solicitudId)).thenReturn(Optional.of(solicitudPendiente));

        var casoUso = new AprobarReversion(solicitudes);

        assertThrows(IllegalArgumentException.class, () ->
            casoUso.ejecutar(solicitudId, mismoUsuario, true)
        );
    }

    @Test
    @DisplayName("RF-23: rechazar resolución de solicitud ya resuelta")
    void rechazarSolicitudYaResuelta() {
        SolicitudReversionRepository solicitudes = mock(SolicitudReversionRepository.class);

        UUID solicitudId = UUID.randomUUID();
        var solicitudAprobada = new SolicitudReversion(
                solicitudId, UUID.randomUUID(), SolicitudReversion.TipoOperacion.PAGO,
                UUID.randomUUID(), "Motivo", EstadoSolicitudReversion.APROBADA, UUID.randomUUID(), Instant.now(), Instant.now());
        when(solicitudes.buscarPorId(solicitudId)).thenReturn(Optional.of(solicitudAprobada));

        var casoUso = new AprobarReversion(solicitudes);

        assertThrows(IllegalStateException.class, () ->
            casoUso.ejecutar(solicitudId, UUID.randomUUID(), true)
        );
    }
}
