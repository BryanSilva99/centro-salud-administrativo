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
import pe.centrosalud.modules.administration.application.ports.Consultorios;
import pe.centrosalud.modules.administration.application.usecases.ErrorConsultorios;
import pe.centrosalud.modules.administration.application.usecases.GestionarConsultorios;
import pe.centrosalud.modules.administration.application.usecases.GestionarServicios;
import pe.centrosalud.modules.administration.application.usecases.GestionarServicios.DatosModalidad;
import pe.centrosalud.modules.administration.domain.Consultorio;
import pe.centrosalud.modules.identity.application.ports.HashContrasenas;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.servlet.session.cookie.secure=false")
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class ConsultoriosHttpTest {
  static final String SCHEMA = "test_consultorios_" + UUID.randomUUID().toString().replace("-", "");

  @DynamicPropertySource
  static void schema(DynamicPropertyRegistry registry) {
    registry.add("spring.flyway.default-schema", () -> SCHEMA);
    registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
  }

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  @Autowired HashContrasenas hashes;
  @Autowired GestionarConsultorios gestionar;
  @Autowired GestionarServicios gestionarServicios;
  @Autowired Consultorios consultorios;
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
        "TRUNCATE auditoria_consultorios,consultorio_servicios,consultorios,auditoria_servicios,servicio_modalidades,servicios,spring_session,intentos_login,cuentas CASCADE");
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

  String nuevo(String codigo) {
    return "{\"codigo\":\""
        + codigo
        + "\",\"nombre\":\"Consultorio de prueba\",\"serviciosCompatibles\":[\""
        + servicioId
        + "\"]}";
  }

  HttpResponse<String> crear(String codigo) throws Exception {
    return send("POST", "/api/consultorios", nuevo(codigo), true);
  }

  Consultorio cargar(String respuesta) {
    return consultorios.buscarPorId(UUID.fromString(read(respuesta, "$.id"))).orElseThrow();
  }

  int contar(String tabla) {
    return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
  }

  String edicion(
      Consultorio c,
      String codigo,
      String nombre,
      boolean activo,
      Set<UUID> servicios,
      long version) {
    String lista =
        servicios.stream().map(id -> "\"" + id + "\"").reduce((a, b) -> a + "," + b).orElse("");
    return "{\"codigo\":\""
        + codigo
        + "\",\"nombre\":\""
        + nombre
        + "\",\"activo\":"
        + activo
        + ",\"serviciosCompatibles\":["
        + lista
        + "],\"version\":"
        + version
        + "}";
  }

  @Test
  void administradorCreaListaYConsultaConAuditoria() throws Exception {
    UUID adm = ingresar("ADM");
    var respuesta = crear("C-100");
    assertEquals(201, respuesta.statusCode(), respuesta.body());
    assertEquals(0, (Integer) read(respuesta.body(), "$.version"));
    assertTrue((Boolean) read(respuesta.body(), "$.activo"));
    var consultorio = cargar(respuesta.body());
    assertEquals(
        Set.of(servicioId.toString()),
        Set.copyOf((List<String>) read(respuesta.body(), "$.serviciosCompatibles")));
    assertEquals(201, crear("C-200").statusCode());
    var listado = send("GET", "/api/consultorios", null, false);
    assertEquals(200, listado.statusCode());
    assertEquals(List.of("C-100", "C-200"), read(listado.body(), "$[*].codigo"));
    assertEquals(
        200, send("GET", "/api/consultorios/" + consultorio.id(), null, false).statusCode());
    assertEquals(
        404, send("GET", "/api/consultorios/" + UUID.randomUUID(), null, false).statusCode());
    var auditoria =
        jdbc.queryForMap(
            "SELECT actor_id,tipo,anterior FROM auditoria_consultorios WHERE consultorio_id=?",
            consultorio.id());
    assertEquals(adm, auditoria.get("actor_id"));
    assertEquals("CREACION", auditoria.get("tipo"));
    assertNull(auditoria.get("anterior"));
  }

  @Test
  void editarCambiaDatosConVersionYAuditoria() throws Exception {
    ingresar("ADM");
    var consultorio = cargar(crear("C-1").body());
    var editado =
        send(
            "PUT",
            "/api/consultorios/" + consultorio.id(),
            edicion(consultorio, "C-1", "Renombrado", true, Set.of(), 0),
            true);
    assertEquals(200, editado.statusCode(), editado.body());
    assertEquals(1, (Integer) read(editado.body(), "$.version"));
    assertEquals("Renombrado", read(editado.body(), "$.nombre"));
    assertEquals(List.of(), read(editado.body(), "$.serviciosCompatibles"));
    var obsoleta =
        send(
            "PUT",
            "/api/consultorios/" + consultorio.id(),
            edicion(consultorio, "C-1", "Otro", true, Set.of(), 0),
            true);
    assertEquals(409, obsoleta.statusCode());
    assertEquals("CONFLICTO", read(obsoleta.body(), "$.code"));
    var actual = consultorios.buscarPorId(consultorio.id()).orElseThrow();
    assertEquals(
        200,
        send(
                "PUT",
                "/api/consultorios/" + consultorio.id(),
                edicion(actual, "C-1", "Renombrado", false, Set.of(), 1),
                true)
            .statusCode());
    assertFalse(consultorios.buscarPorId(consultorio.id()).orElseThrow().activo());
    assertEquals(3, contar("auditoria_consultorios"));
    // Guardar sin cambios no incrementa la versión ni audita.
    actual = consultorios.buscarPorId(consultorio.id()).orElseThrow();
    assertEquals(
        200,
        send(
                "PUT",
                "/api/consultorios/" + consultorio.id(),
                edicion(actual, "C-1", "Renombrado", false, Set.of(), 2),
                true)
            .statusCode());
    assertEquals(2, consultorios.buscarPorId(consultorio.id()).orElseThrow().version());
    assertEquals(3, contar("auditoria_consultorios"));
  }

  @Test
  void rechazaCodigoDuplicadoYServicioInvalido() throws Exception {
    ingresar("ADM");
    assertEquals(201, crear("C-1").statusCode());
    var duplicado = crear("c-1");
    assertEquals(409, duplicado.statusCode());
    assertEquals("CODIGO_DUPLICADO", read(duplicado.body(), "$.code"));
    var invalido =
        send(
            "POST",
            "/api/consultorios",
            "{\"codigo\":\"C-2\",\"nombre\":\"Consultorio\",\"serviciosCompatibles\":[\""
                + UUID.randomUUID()
                + "\"]}",
            true);
    assertEquals(400, invalido.statusCode());
    assertEquals("SERVICIO_INVALIDO", read(invalido.body(), "$.code"));
    assertEquals(1, contar("consultorios"));
  }

  @Test
  void diezAltasConcurrentesDelMismoCodigoCreanUnSoloConsultorio() throws Exception {
    UUID adm = cuenta("ADM");
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(10)) {
      List<Future<String>> tareas = new ArrayList<>();
      for (int i = 0; i < 10; i++)
        tareas.add(
            pool.submit(
                () -> {
                  inicio.await();
                  try {
                    gestionar.crear(adm, "UNICO", "Consultorio", Set.of(servicioId));
                    return "OK";
                  } catch (ErrorConsultorios e) {
                    return e.codigo().name();
                  }
                }));
      inicio.countDown();
      List<String> resultados = new ArrayList<>();
      for (var tarea : tareas) resultados.add(tarea.get(15, TimeUnit.SECONDS));
      assertEquals(1, Collections.frequency(resultados, "OK"), resultados.toString());
      assertEquals(9, Collections.frequency(resultados, "CODIGO_DUPLICADO"), resultados.toString());
    }
    assertEquals(1, contar("consultorios"));
  }

  @Test
  void diezEdicionesConcurrentesConLaMismaVersionSoloAplicanUna() throws Exception {
    UUID adm = cuenta("ADM");
    var consultorio = gestionar.crear(adm, "C-1", "Consultorio", Set.of(servicioId));
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
                        adm, consultorio.id(), "C-1", nombre, true, Set.of(servicioId), 0);
                    return "OK";
                  } catch (ErrorConsultorios e) {
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
    assertEquals(1, consultorios.buscarPorId(consultorio.id()).orElseThrow().version());
  }

  @Test
  void falloDeAuditoriaRevierteAltaYEdicion() throws Exception {
    ingresar("ADM");
    var consultorio = cargar(crear("C-1").body());
    jdbc.execute(
        "ALTER TABLE auditoria_consultorios ADD CONSTRAINT simular_fallo CHECK(false) NOT VALID");
    try {
      assertEquals(503, crear("C-2").statusCode());
      assertEquals(1, contar("consultorios"));
      assertEquals(
          503,
          send(
                  "PUT",
                  "/api/consultorios/" + consultorio.id(),
                  edicion(consultorio, "C-1", "Otro", true, Set.of(), 0),
                  true)
              .statusCode());
      assertEquals(consultorio, consultorios.buscarPorId(consultorio.id()).orElseThrow());
    } finally {
      jdbc.execute("ALTER TABLE auditoria_consultorios DROP CONSTRAINT simular_fallo");
    }
  }

  @Test
  void soloAdministracionGestionaConsultorios() throws Exception {
    UUID adm = cuenta("ADM");
    var consultorio = gestionar.crear(adm, "C-1", "Consultorio", Set.of(servicioId));
    String editar = "/api/consultorios/" + consultorio.id();
    for (String rol : List.of("REC", "CAJ", "PRO", "")) {
      if (rol.isEmpty()) ingresar();
      else ingresar(rol);
      assertEquals(403, send("GET", "/api/consultorios", null, false).statusCode(), rol);
      assertEquals(403, send("GET", editar, null, false).statusCode(), rol);
      assertEquals(403, crear("C-2").statusCode(), rol);
      assertEquals(403, send("PUT", editar, "{}", true).statusCode(), rol);
    }
    assertEquals(1, contar("consultorios"));
    ingresar("ADM", "REC");
    assertEquals(201, crear("C-2").statusCode());
  }

  @Test
  void exigeSesionCsrfYPermisosVigentes() throws Exception {
    client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    token();
    assertEquals(401, send("GET", "/api/consultorios", null, false).statusCode());
    assertEquals(401, crear("C-1").statusCode());
    UUID adm = ingresar("ADM");
    assertEquals(403, send("POST", "/api/consultorios", nuevo("C-1"), false).statusCode());
    jdbc.update("DELETE FROM cuenta_roles WHERE cuenta_id=?", adm);
    assertEquals(403, crear("C-1").statusCode());
    UUID otra = ingresar("ADM");
    jdbc.update("UPDATE cuentas SET activa=false WHERE id=?", otra);
    assertEquals(401, crear("C-1").statusCode());
    assertEquals(0, contar("consultorios"));
  }

  @Test
  void rechazaDatosInvalidos() throws Exception {
    ingresar("ADM");
    for (String body :
        List.of(
            "{\"codigo\":\"\",\"nombre\":\"Consultorio\",\"serviciosCompatibles\":[]}",
            "{\"codigo\":\"C-1\",\"nombre\":\"\",\"serviciosCompatibles\":[]}",
            "{\"codigo\":\"C-1\",\"nombre\":\"Consultorio\"}",
            "{\"codigo\":")) {
      assertEquals(400, send("POST", "/api/consultorios", body, true).statusCode(), body);
    }
    assertEquals(0, contar("consultorios"));
  }
}
