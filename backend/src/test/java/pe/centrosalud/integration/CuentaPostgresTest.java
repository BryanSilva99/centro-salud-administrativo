package pe.centrosalud.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.centrosalud.modules.identity.application.usecases.ConsultarCuenta;
import pe.centrosalud.modules.identity.application.usecases.CuentaNoEncontrada;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class CuentaPostgresTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired ConsultarCuenta consultar;
  @Autowired MockMvc mvc;
  @Autowired pe.centrosalud.adapters.inbound.http.identity.CuentaController controller;

  @Test
  void recorreControladorCasoDeUsoYPostgres() throws Exception {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO cuentas (id, identificador, activa) VALUES (?, ?, ?)",
        id,
        "recorrido-" + id,
        true);
    // Solo en esta prueba aislamos los filtros; la cadena real se prueba debajo.
    var http =
        org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
            .build();
    http.perform(get("/api/cuentas/{id}", id))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.identificador")
                .value("recorrido-" + id));
  }

  @Test
  void laBaseRechazaIdentificadoresDuplicados() {
    String nombre = "unico-" + UUID.randomUUID();
    jdbc.update(
        "INSERT INTO cuentas (id, identificador, activa) VALUES (?, ?, ?)",
        UUID.randomUUID(),
        nombre,
        true);
    assertThrows(
        org.springframework.dao.DuplicateKeyException.class,
        () ->
            jdbc.update(
                "INSERT INTO cuentas (id, identificador, activa) VALUES (?, ?, ?)",
                UUID.randomUUID(),
                nombre,
                true));
  }

  @Test
  void consultaUnaFilaRealYConservaSuEstado() {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO cuentas (id, identificador, activa) VALUES (?, ?, ?)",
        id,
        "prueba-" + id,
        false);
    var cuenta = consultar.ejecutar(id);
    assertEquals(id, cuenta.id());
    assertEquals("prueba-" + id, cuenta.identificador());
    assertFalse(cuenta.activa());
  }

  @Test
  void identificaAusenciaEnPostgres() {
    assertThrows(CuentaNoEncontrada.class, () -> consultar.ejecutar(UUID.randomUUID()));
  }

  @Test
  void noExponeCuentasSinAutenticacion() throws Exception {
    mvc.perform(get("/api/cuentas/{id}", UUID.randomUUID())).andExpect(status().isUnauthorized());
  }

  @Test
  void tampocoHabilitaUnRolSinImplementarLaPoliticaDeAcceso() throws Exception {
    mvc.perform(get("/api/cuentas/{id}", UUID.randomUUID()).with(user("prueba").roles("ADM")))
        .andExpect(status().isUnauthorized());
  }
}
