package pe.centrosalud.integration.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("RF-19: POST /api/billing/cargos requiere autenticación")
    void registrarCargoRequiereAutenticacion() throws Exception {
        mockMvc.perform(post("/api/billing/cargos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"citaId\":\"" + java.util.UUID.randomUUID() + "\",\"pacienteId\":\"" + java.util.UUID.randomUUID() + "\",\"importe\":\"50.00\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RF-20: POST /api/billing/sesiones requiere autenticación")
    void abrirSesionRequiereAutenticacion() throws Exception {
        mockMvc.perform(post("/api/billing/sesiones")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cajeroId\":\"" + java.util.UUID.randomUUID() + "\",\"fondoInicial\":\"100.00\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RF-21: POST /api/billing/pagos requiere autenticación")
    void registrarPagoRequiereAutenticacion() throws Exception {
        mockMvc.perform(post("/api/billing/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cargoId\":\"" + java.util.UUID.randomUUID() + "\",\"sesionCajaId\":\"" + java.util.UUID.randomUUID() + "\",\"importe\":\"50.00\",\"medio\":\"EFECTIVO\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RF-22: GET /api/billing/cargos/{id}/saldo requiere autenticación")
    void consultarSaldoRequiereAutenticacion() throws Exception {
        mockMvc.perform(get("/api/billing/cargos/" + java.util.UUID.randomUUID() + "/saldo"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RF-24: POST /api/billing/sesiones/{id}/cerrar requiere autenticación")
    void cerrarCajaRequiereAutenticacion() throws Exception {
        mockMvc.perform(post("/api/billing/sesiones/" + java.util.UUID.randomUUID() + "/cerrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"efectivoDeclarado\":\"150.00\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RF-27: GET /api/billing/reportes/economico requiere autenticación")
    void reporteEconomicoRequiereAutenticacion() throws Exception {
        mockMvc.perform(get("/api/billing/reportes/economico")
                .param("fechaInicio", "2026-10-01")
                .param("fechaFin", "2026-10-31"))
            .andExpect(status().isUnauthorized());
    }
}
