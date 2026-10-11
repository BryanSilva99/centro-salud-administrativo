package pe.centrosalud.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pe.centrosalud.modules.identity.application.ports.*;
import pe.centrosalud.modules.identity.application.usecases.*;

@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class CredencialesPostgresTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired HashContrasenas hashes;
  @Autowired VerificarCredenciales verificar;
  @Autowired ConsultarCredenciales consultar;

  @Test
  void verificaCuentaRealYRechazaLosTresFallosSinRevelarSuCausa() {
    UUID id = UUID.randomUUID();
    String nombre = "prueba-" + id, clave = UUID.randomUUID().toString();
    jdbc.update(
        "INSERT INTO cuentas (id, identificador, activa, contrasena_hash) VALUES (?, ?, ?, ?)",
        id,
        nombre,
        true,
        hashes.generar(clave));
    assertEquals(id, verificar.ejecutar(nombre, clave).id());
    String error =
        assertThrows(CredencialesInvalidas.class, () -> verificar.ejecutar(nombre, clave + "x"))
            .getMessage();
    assertEquals(
        error,
        assertThrows(CredencialesInvalidas.class, () -> verificar.ejecutar("ausente-" + id, clave))
            .getMessage());
    jdbc.update("UPDATE cuentas SET activa = false WHERE id = ?", id);
    // Una nueva consulta debe observar el estado persistido, no una entidad obsoleta del contexto
    // JPA.
    entityManager.clear();
    assertEquals(
        error,
        assertThrows(CredencialesInvalidas.class, () -> verificar.ejecutar(nombre, clave))
            .getMessage());
  }

  @Autowired jakarta.persistence.EntityManager entityManager;

  @Test
  void cuentaSinCredencialNoPuedeIngresarYLaBusquedaEsExacta() {
    UUID id = UUID.randomUUID();
    String nombre = "Nombre-" + id;
    jdbc.update(
        "INSERT INTO cuentas (id, identificador, activa) VALUES (?, ?, ?)", id, nombre, true);
    assertTrue(consultar.buscarPorIdentificador(nombre).isPresent());
    assertTrue(
        consultar.buscarPorIdentificador(nombre.toLowerCase(java.util.Locale.ROOT)).isEmpty());
    assertTrue(consultar.buscarPorIdentificador(" " + nombre).isEmpty());
    assertThrows(
        CredencialesInvalidas.class,
        () -> verificar.ejecutar(nombre, UUID.randomUUID().toString()));
  }

  @Test
  void laBaseRechazaUnaContrasenaEnTextoPlano() {
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "INSERT INTO cuentas (id, identificador, activa, contrasena_hash) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(),
                "invalida-" + UUID.randomUUID(),
                true,
                "no-es-un-hash"));
  }
}
