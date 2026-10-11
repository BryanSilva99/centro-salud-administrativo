package pe.centrosalud.integration;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pe.centrosalud.modules.administration.application.ports.FichasProfesionales;
import pe.centrosalud.modules.administration.application.usecases.ErrorProfesionales;
import pe.centrosalud.modules.administration.application.usecases.GestionarProfesionales;
import pe.centrosalud.modules.administration.application.usecases.GestionarServicios;
import pe.centrosalud.modules.administration.application.usecases.GestionarServicios.DatosModalidad;
import pe.centrosalud.modules.administration.domain.FichaProfesional;
import pe.centrosalud.modules.identity.application.ports.HashContrasenas;

/** RF-07. El alta básica y el vínculo con cuentas (RF-02) ya se prueban en RolesAdminHttpTest. */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.servlet.session.cookie.secure=false")
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class ProfesionalesHttpTest {
  static final String SCHEMA =
      "test_profesionales_" + UUID.randomUUID().toString().replace("-", "");

  @DynamicPropertySource
  static void schema(DynamicPropertyRegistry registry) {
    registry.add("spring.flyway.default-schema", () -> SCHEMA);
    registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
  }

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  @Autowired HashContrasenas hashes;
  @Autowired GestionarProfesionales gestionar;
  @Autowired GestionarServicios gestionarServicios;
  @Autowired FichasProfesionales fichas;
  String password, csrf;
  HttpClient client;
  UUID servicioId;

  @BeforeEach
  void preparar() {
    password = UUID.randomUUID().toString();
    servicioId =
        gestionarServicios
            .crear(
                UUID.randomUUID(),
                "Consulta-" + UUID.randomUUID(),
                30,
                List.of(new DatosModalidad(null, "Particular", true, new BigDecimal("40"), true)))
            .id();
  }

  @AfterEach
  void limpiar() {
    jdbc.execute(
        "TRUNCATE auditoria_profesionales,profesional_servicios,profesionales,auditoria_servicios,servicio_modalidades,servicios,spring_session,intentos_login,cuentas CASCADE");
  }

  @AfterAll
  static void quitarSchema(@Autowired JdbcTemplate jdbc) {
    jdbc.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
  }

  UUID cuenta(String... roles) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO cuentas(id,identificador,activa,contrasena_hash) VALUES (?,?,true,?)",
        id,
        id.toString(),
        hashes.generar(password));
    for (String rol : roles)
      jdbc.update("INSERT INTO cuenta_roles(cuenta_id,rol) VALUES (?,?)", id, rol);
    return id;
  }

  UUID ingresar(String... roles) throws Exception {
    UUID id = cuenta(roles);
    client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    token();
    assertEquals(
        200,
        send(
                "POST",
                "/api/auth/login",
                "{\"identificador\":\"" + id + "\",\"contrasena\":\"" + password + "\"}",
                true)
            .statusCode());
    token();
    return id;
  }

  HttpResponse<String> send(String method, String path, String body, boolean token)
      throws Exception {
    var b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
    if (token) b.header("X-CSRF-TOKEN", csrf);
    if (body != null) b.header("Content-Type", "application/json");
    return client.send(
        b.method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  void token() throws Exception {
    csrf = read(send("GET", "/api/auth/csrf", null, false).body(), "$.token");
  }

  String nuevaFicha(String identificacion) {
    return "{\"identificacion\":\""
        + identificacion
        + "\",\"nombre\":\"Profesional de prueba\",\"funcion\":\"Medicina general\",\"serviciosHabilitados\":[\""
        + servicioId
        + "\"]}";
  }

  HttpResponse<String> crear(String identificacion) throws Exception {
    return send("POST", "/api/profesionales/fichas", nuevaFicha(identificacion), true);
  }

  FichaProfesional cargar(String respuesta) {
    return fichas.buscarPorId(UUID.fromString(read(respuesta, "$.id"))).orElseThrow();
  }

  int contar(String tabla) {
    return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
  }

  String edicion(
      FichaProfesional f,
      String identificacion,
      String nombre,
      boolean activa,
      Set<UUID> servicios,
      long version) {
    String lista =
        servicios.stream().map(id -> "\"" + id + "\"").reduce((a, b) -> a + "," + b).orElse("");
    return "{\"identificacion\":\""
        + identificacion
        + "\",\"nombre\":\""
        + nombre
        + "\",\"funcion\":\"Medicina general\",\"activa\":"
        + activa
        + ",\"serviciosHabilitados\":["
        + lista
        + "],\"version\":"
        + version
        + "}";
  }

  @Test
  void administradorCreaConServiciosHabilitadosYAuditoriaSinDatosPersonales() throws Exception {
    UUID adm = ingresar("ADM");
    var respuesta = crear("P-" + UUID.randomUUID());
    assertEquals(201, respuesta.statusCode(), respuesta.body());
    assertEquals(0, (Integer) read(respuesta.body(), "$.version"));
    var ficha = cargar(respuesta.body());
    assertEquals(Set.of(servicioId), ficha.serviciosHabilitados());
    var auditoria =
        jdbc.queryForMap(
            "SELECT actor_id,tipo,anterior,nuevo::text AS nuevo FROM auditoria_profesionales WHERE profesional_id=?",
            ficha.id());
    assertEquals(adm, auditoria.get("actor_id"));
    assertEquals("CREACION", auditoria.get("tipo"));
    assertNull(auditoria.get("anterior"));
    assertFalse(
        auditoria.get("nuevo").toString().contains("Profesional de prueba"),
        "La auditoría no debe contener el nombre");
    assertFalse(
        auditoria.get("nuevo").toString().contains(ficha.identificacion()),
        "La auditoría no debe contener la identificación");
  }

  @Test
  void editarCambiaDatosYServiciosConVersionYAuditoria() throws Exception {
    ingresar("ADM");
    var ficha = cargar(crear("P-" + UUID.randomUUID()).body());
    var editada =
        send(
            "PUT",
            "/api/profesionales/fichas/" + ficha.id(),
            edicion(ficha, ficha.identificacion(), "Renombrado", true, Set.of(), 0),
            true);
    assertEquals(200, editada.statusCode(), editada.body());
    assertEquals(1, (Integer) read(editada.body(), "$.version"));
    assertEquals("Renombrado", read(editada.body(), "$.nombre"));
    assertEquals(List.of(), read(editada.body(), "$.serviciosHabilitados"));
    var obsoleta =
        send(
            "PUT",
            "/api/profesionales/fichas/" + ficha.id(),
            edicion(ficha, ficha.identificacion(), "Otro", true, Set.of(), 0),
            true);
    assertEquals(409, obsoleta.statusCode());
    assertEquals("CONFLICTO", read(obsoleta.body(), "$.code"));
    var actual = fichas.buscarPorId(ficha.id()).orElseThrow();
    assertEquals(
        200,
        send(
                "PUT",
                "/api/profesionales/fichas/" + ficha.id(),
                edicion(actual, actual.identificacion(), "Renombrado", false, Set.of(), 1),
                true)
            .statusCode());
    assertFalse(fichas.buscarPorId(ficha.id()).orElseThrow().activa());
    assertEquals(3, contar("auditoria_profesionales"));
    // Guardar sin cambios no incrementa la versión ni audita.
    actual = fichas.buscarPorId(ficha.id()).orElseThrow();
    assertEquals(
        200,
        send(
                "PUT",
                "/api/profesionales/fichas/" + ficha.id(),
                edicion(actual, actual.identificacion(), "Renombrado", false, Set.of(), 2),
                true)
            .statusCode());
    assertEquals(2, fichas.buscarPorId(ficha.id()).orElseThrow().version());
    assertEquals(3, contar("auditoria_profesionales"));
  }

  @Test
  void rechazaServicioInvalidoAlEditar() throws Exception {
    ingresar("ADM");
    var ficha = cargar(crear("P-" + UUID.randomUUID()).body());
    var invalido =
        send(
            "PUT",
            "/api/profesionales/fichas/" + ficha.id(),
            edicion(
                ficha, ficha.identificacion(), ficha.nombre(), true, Set.of(UUID.randomUUID()), 0),
            true);
    assertEquals(400, invalido.statusCode());
    assertEquals("SERVICIO_INVALIDO", read(invalido.body(), "$.code"));
  }

  @Test
  void diezEdicionesConcurrentesConLaMismaVersionSoloAplicanUna() throws Exception {
    UUID adm = cuenta("ADM");
    var ficha =
        gestionar.crear(
            adm, "P-" + UUID.randomUUID(), "Profesional", "Medicina", Set.of(servicioId));
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(10)) {
      List<Future<String>> tareas = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        String nombre = "Edicion-" + i;
        tareas.add(
            pool.submit(
                () -> {
                  inicio.await();
                  try {
                    gestionar.actualizar(
                        adm,
                        ficha.id(),
                        ficha.identificacion(),
                        nombre,
                        "Medicina",
                        true,
                        Set.of(servicioId),
                        0);
                    return "OK";
                  } catch (ErrorProfesionales e) {
                    return e.codigo().name();
                  }
                }));
      }
      inicio.countDown();
      List<String> resultados = new ArrayList<>();
      for (var tarea : tareas) resultados.add(tarea.get(15, TimeUnit.SECONDS));
      assertEquals(1, Collections.frequency(resultados, "OK"), resultados.toString());
      assertEquals(9, Collections.frequency(resultados, "CONFLICTO"), resultados.toString());
    }
    assertEquals(1, fichas.buscarPorId(ficha.id()).orElseThrow().version());
  }

  @Test
  void falloDeAuditoriaRevierteAltaYEdicion() throws Exception {
    ingresar("ADM");
    var ficha = cargar(crear("P-" + UUID.randomUUID()).body());
    jdbc.execute(
        "ALTER TABLE auditoria_profesionales ADD CONSTRAINT simular_fallo CHECK(false) NOT VALID");
    try {
      assertEquals(503, crear("P-" + UUID.randomUUID()).statusCode());
      assertEquals(1, contar("profesionales"));
      assertEquals(
          503,
          send(
                  "PUT",
                  "/api/profesionales/fichas/" + ficha.id(),
                  edicion(ficha, ficha.identificacion(), "Otro", true, Set.of(), 0),
                  true)
              .statusCode());
      assertEquals(ficha, fichas.buscarPorId(ficha.id()).orElseThrow());
    } finally {
      jdbc.execute("ALTER TABLE auditoria_profesionales DROP CONSTRAINT simular_fallo");
    }
  }

  @Test
  void soloAdministracionGestionaProfesionales() throws Exception {
    UUID adm = cuenta("ADM");
    var ficha =
        gestionar.crear(
            adm, "P-" + UUID.randomUUID(), "Profesional", "Medicina", Set.of(servicioId));
    String editar = "/api/profesionales/fichas/" + ficha.id();
    for (String rol : List.of("REC", "CAJ", "PRO", "")) {
      if (rol.isEmpty()) ingresar();
      else ingresar(rol);
      assertEquals(403, send("GET", "/api/profesionales/fichas", null, false).statusCode(), rol);
      assertEquals(403, send("GET", editar, null, false).statusCode(), rol);
      assertEquals(403, crear("P-" + UUID.randomUUID()).statusCode(), rol);
      assertEquals(403, send("PUT", editar, "{}", true).statusCode(), rol);
    }
    assertEquals(1, contar("profesionales"));
    ingresar("ADM", "REC");
    assertEquals(201, crear("P-" + UUID.randomUUID()).statusCode());
  }

  @Test
  void exigeSesionCsrfYPermisosVigentes() throws Exception {
    client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    token();
    assertEquals(401, send("GET", "/api/profesionales/fichas", null, false).statusCode());
    assertEquals(401, crear("P-" + UUID.randomUUID()).statusCode());
    UUID adm = ingresar("ADM");
    assertEquals(
        403,
        send("POST", "/api/profesionales/fichas", nuevaFicha("P-" + UUID.randomUUID()), false)
            .statusCode());
    jdbc.update("DELETE FROM cuenta_roles WHERE cuenta_id=?", adm);
    assertEquals(403, crear("P-" + UUID.randomUUID()).statusCode());
    UUID otra = ingresar("ADM");
    jdbc.update("UPDATE cuentas SET activa=false WHERE id=?", otra);
    assertEquals(401, crear("P-" + UUID.randomUUID()).statusCode());
    assertEquals(0, contar("profesionales"));
  }
}
