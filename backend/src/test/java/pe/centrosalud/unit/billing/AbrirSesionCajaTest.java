package pe.centrosalud.unit.billing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import pe.centrosalud.modules.billing.application.ports.SesionCajaRepository;
import pe.centrosalud.modules.billing.application.usecases.AbrirSesionCaja;
import pe.centrosalud.modules.billing.domain.EstadoSesionCaja;

class AbrirSesionCajaTest {

    @Test
    @DisplayName("RF-20/CA-1: abrir sesión con fondo inicial válido")
    void abrirSesionValida() {
        SesionCajaRepository sesiones = mock(SesionCajaRepository.class);
        when(sesiones.buscarSesionAbiertaPorCajero(any())).thenReturn(Optional.empty());
        when(sesiones.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        var casoUso = new AbrirSesionCaja(sesiones);
        UUID cajeroId = UUID.randomUUID();

        var sesion = casoUso.ejecutar(cajeroId, new BigDecimal("100.00"));

        assertNotNull(sesion);
        assertEquals(cajeroId, sesion.cajeroId());
        assertEquals(new BigDecimal("100.00"), sesion.fondoInicial());
        assertEquals(EstadoSesionCaja.ABIERTA, sesion.estado());
        assertNotNull(sesion.fechaApertura());
    }

    @Test
    @DisplayName("RF-20/CA-2: rechazar segunda sesión abierta para el mismo cajero")
    void rechazarSesionDuplicada() {
        SesionCajaRepository sesiones = mock(SesionCajaRepository.class);
        UUID cajeroId = UUID.randomUUID();
        var sesionAbierta = new pe.centrosalud.modules.billing.domain.SesionCaja(
                UUID.randomUUID(), cajeroId, new BigDecimal("50.00"), EstadoSesionCaja.ABIERTA,
                java.time.Instant.now(), null, null, null, null);
        when(sesiones.buscarSesionAbiertaPorCajero(cajeroId)).thenReturn(Optional.of(sesionAbierta));

        var casoUso = new AbrirSesionCaja(sesiones);

        assertThrows(IllegalStateException.class, () ->
            casoUso.ejecutar(cajeroId, new BigDecimal("100.00"))
        );
    }

    @Test
    @DisplayName("RF-20/CA-1: rechazar fondo inicial negativo")
    void rechazarFondoNegativo() {
        SesionCajaRepository sesiones = mock(SesionCajaRepository.class);
        var casoUso = new AbrirSesionCaja(sesiones);

        assertThrows(IllegalArgumentException.class, () ->
            casoUso.ejecutar(UUID.randomUUID(), new BigDecimal("-50"))
        );
    }
}
