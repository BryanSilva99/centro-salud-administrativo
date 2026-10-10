package pe.centrosalud.unit;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.centrosalud.adapters.inbound.http.identity.CuentaController;
import pe.centrosalud.modules.identity.application.usecases.ConsultarCuenta;
import pe.centrosalud.modules.identity.domain.Cuenta;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Prueba aislada de la traducción HTTP; no representa acceso habilitado. */
class CuentaControllerTest {
    @Test void devuelveSoloLosDatosPublicadosPorElContrato() throws Exception {
        var cuenta = new Cuenta(UUID.randomUUID(), "bryan", true);
        var mvc = MockMvcBuilders.standaloneSetup(new CuentaController(
                new ConsultarCuenta(id -> Optional.of(cuenta)))).build();
        mvc.perform(get("/api/cuentas/{id}", cuenta.id()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"id\":\"" + cuenta.id() + "\",\"identificador\":\"bryan\",\"activa\":true}"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void devuelve404CuandoNoExiste() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new CuentaController(
                new ConsultarCuenta(id -> Optional.empty()))).build();
        mvc.perform(get("/api/cuentas/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No se encontró la cuenta solicitada"));
    }
    @Test void rechazaIdMalFormadoAntesDelCasoDeUso() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new CuentaController(
                new ConsultarCuenta(id -> { throw new AssertionError("No debe consultar"); }))).build();
        mvc.perform(get("/api/cuentas/no-es-uuid")).andExpect(status().isBadRequest());
    }
}
