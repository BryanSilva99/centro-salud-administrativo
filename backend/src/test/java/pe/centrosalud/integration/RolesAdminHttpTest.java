package pe.centrosalud.integration;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.jupiter.api.Assertions.*;

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
import pe.centrosalud.modules.identity.application.ports.*;
import pe.centrosalud.modules.identity.application.roles.*;
import pe.centrosalud.modules.identity.domain.Rol;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.servlet.session.cookie.secure=false")
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class RolesAdminHttpTest {
  static final String SCHEMA = "test_roles_" + UUID.randomUUID().toString().replace("-", "");

  @DynamicPropertySource
  static void schema(DynamicPropertyRegistry registry) {
    registry.add("spring.flyway.default-schema", () -> SCHEMA);
    registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
  }

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  @Autowired HashContrasenas hashes;
  @Autowired CambiarRoles cambiar;
  @Autowired DirectorioRoles directorio;
  @Autowired pe.centrosalud.modules.identity.application.usecases.GestionarCuentas gestionar;
  @Autowired HistorialCuentas historialCuentas;
  UUID actor, target;
  String password, csrf;
  HttpClient client;

  @BeforeEach
  void preparar() throws Exception {
    password = UUID.randomUUID().toString();
    actor = crear(Rol.ADM);
    target = crear(Rol.REC);
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
                "{\"identificador\":\"" + actor + "\",\"contrasena\":\"" + password + "\"}",
                true)
            .statusCode());
    token();
  }

  UUID crear(Rol rol) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO cuentas(id,identificador,activa,contrasena_hash) VALUES (?,?,true,?)",
        id,
        id.toString(),
        hashes.generar(password));
    jdbc.update("INSERT INTO cuenta_roles(cuenta_id,rol) VALUES (?,?)", id, rol.name());
    return id;
  }

  @AfterEach
  void limpiar() {
    jdbc.execute(
        "TRUNCATE profesionales,auditoria_cuentas,auditoria_roles,spring_session,intentos_login,cuentas CASCADE");
  }

  @AfterAll
  static void quitarSchema(@Autowired JdbcTemplate jdbc) {
    jdbc.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
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

  HttpResponse<String> put(UUID id, String roles, long version) throws Exception {
    return send(
        "PUT",
        "/api/cuentas/" + id + "/roles",
        "{\"roles\":" + roles + ",\"version\":" + version + "}",
        true);
  }

  @Test
  void cambioPersisteAuditaYDetectaEdicionObsoleta() throws Exception {
    long v = directorio.cargar(target).version();
    var result = put(target, "[\"REC\",\"CAJ\"]", v);
    assertEquals(200, result.statusCode(), result.body());
    assertEquals(Set.of(Rol.REC, Rol.CAJ), directorio.cargar(target).roles());
    var audit = directorio.historial(target, 0).elementos();
    assertEquals(1, audit.size());
    assertEquals(actor, audit.getFirst().actorId());
    assertEquals(Set.of(Rol.REC), audit.getFirst().anteriores());
    assertEquals(409, put(target, "[]", v).statusCode());
    assertEquals(200, send("GET", "/api/cuentas?busqueda=" + target, null, false).statusCode());
    assertEquals(
        200, send("GET", "/api/cuentas/" + target + "/roles/auditoria", null, false).statusCode());
    long nueva = directorio.cargar(target).version();
    assertEquals(200, put(target, "[\"CAJ\",\"REC\"]", nueva).statusCode());
    assertEquals(1, directorio.historial(target, 0).elementos().size());
    assertEquals(200, put(target, "[]", nueva).statusCode());
    assertTrue(directorio.cargar(target).roles().isEmpty());
  }

  @Test
  void protegeUltimoAdministradorActivo() throws Exception {
    UUID inactivo = crear(Rol.ADM);
    jdbc.update("UPDATE cuentas SET activa=false WHERE id=?", inactivo);
    var result = put(actor, "[]", directorio.cargar(actor).version());
    assertEquals(409, result.statusCode());
    assertEquals("ULTIMO_ADMIN", read(result.body(), "$.code"));
    assertTrue(directorio.historial(actor, 0).elementos().isEmpty());
  }

  @Test
  void requierePermisoActualCsrfYDatosValidos() throws Exception {
    String path = "/api/cuentas/" + target + "/roles";
    assertEquals(403, send("PUT", path, "{\"roles\":[],\"version\":1}", false).statusCode());
    assertEquals(400, send("PUT", path, "{\"roles\":[\"ROOT\"],\"version\":1}", true).statusCode());
    assertEquals(400, send("PUT", path, "{\"roles\":[],\"version\":-1}", true).statusCode());
    jdbc.update("DELETE FROM cuenta_roles WHERE cuenta_id=?", actor);
    for (String route : List.of("/api/cuentas", path, path + "/auditoria"))
      assertEquals(403, send("GET", route, null, false).statusCode());
    assertEquals(403, put(target, "[]", 1).statusCode());
    assertEquals(
        ErrorRoles.Codigo.PROHIBIDO,
        assertThrows(ErrorRoles.class, () -> cambiar.ejecutar(actor, target, Set.of(), 1))
            .codigo());
  }

  @Test
  void dosAdministradoresNoPuedenRetirarseSimultaneamente() throws Exception {
    UUID segundo = crear(Rol.ADM);
    long va = directorio.cargar(actor).version(), vb = directorio.cargar(segundo).version();
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var a = pool.submit(() -> retirar(inicio, actor, va));
      var b = pool.submit(() -> retirar(inicio, segundo, vb));
      inicio.countDown();
      assertEquals(
          Set.of("OK", "ULTIMO_ADMIN"),
          Set.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)));
    }
    assertEquals(
        1, jdbc.queryForObject("SELECT count(*) FROM cuenta_roles WHERE rol='ADM'", Integer.class));
  }

  String retirar(CountDownLatch inicio, UUID id, long version) throws Exception {
    inicio.await();
    try {
      cambiar.ejecutar(id, id, Set.of(), version);
      return "OK";
    } catch (ErrorRoles e) {
      return e.codigo().name();
    }
  }

  @Test
  void falloAuditoriaRevierteCambioCompleto() throws Exception {
    var anterior = directorio.cargar(target);
    jdbc.execute("ALTER TABLE auditoria_roles ADD CONSTRAINT simular_fallo CHECK(false) NOT VALID");
    try {
      assertEquals(503, put(target, "[\"CAJ\"]", anterior.version()).statusCode());
      assertEquals(anterior, directorio.cargar(target));
      assertTrue(directorio.historial(target, 0).elementos().isEmpty());
    } finally {
      jdbc.execute("ALTER TABLE auditoria_roles DROP CONSTRAINT simular_fallo");
    }
  }

  HttpResponse<String> crearHttp(String nombre, String clave, String roles) throws Exception {
    return send(
        "POST",
        "/api/cuentas",
        "{\"identificador\":\""
            + nombre
            + "\",\"contrasena\":\""
            + clave
            + "\",\"roles\":"
            + roles
            + "}",
        true);
  }

  HttpResponse<String> editarHttp(UUID id, String nombre, boolean activa, long version)
      throws Exception {
    return send(
        "PUT",
        "/api/cuentas/" + id,
        "{\"identificador\":\""
            + nombre
            + "\",\"activa\":"
            + activa
            + ",\"version\":"
            + version
            + "}",
        true);
  }

  HttpResponse<String> externo(
      HttpClient otro, String method, String path, String body, String token) throws Exception {
    var b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
    if (token != null) b.header("X-CSRF-TOKEN", token);
    if (body != null) b.header("Content-Type", "application/json");
    return otro.send(
        b.method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void creaConHashAuditaYPermiteIniciarSesion() throws Exception {
    String nombre = "nueva." + UUID.randomUUID();
    var respuesta = crearHttp(nombre, password, "[\"REC\",\"CAJ\"]");
    assertEquals(201, respuesta.statusCode(), respuesta.body());
    assertFalse(respuesta.body().contains(password));
    assertFalse(respuesta.body().contains("hash"));
    UUID id = UUID.fromString(read(respuesta.body(), "$.id"));
    String hash =
        jdbc.queryForObject("SELECT contrasena_hash FROM cuentas WHERE id=?", String.class, id);
    assertNotEquals(password, hash);
    assertTrue(hashes.coincide(password, hash));
    var auditoria = historialCuentas.historialCuenta(id, 0).elementos().getFirst();
    assertEquals(actor, auditoria.actorId());
    assertEquals("CREACION", auditoria.tipo());
    assertEquals(Set.of(Rol.REC, Rol.CAJ), auditoria.roles());
    var httpAudit = send("GET", "/api/cuentas/" + id + "/auditoria", null, false);
    assertEquals(200, httpAudit.statusCode());
    assertFalse(httpAudit.body().contains(password));
    assertFalse(httpAudit.body().contains(hash));
    HttpClient otro =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    String token = read(externo(otro, "GET", "/api/auth/csrf", null, null).body(), "$.token");
    assertEquals(
        200,
        externo(
                otro,
                "POST",
                "/api/auth/login",
                "{\"identificador\":\"" + nombre + "\",\"contrasena\":\"" + password + "\"}",
                token)
            .statusCode());
    assertEquals(403, externo(otro, "GET", "/api/cuentas", null, null).statusCode());
  }

  @Test
  void desactivarRevocaSesionYReactivarNoLaResucita() throws Exception {
    HttpClient otro =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    String token = read(externo(otro, "GET", "/api/auth/csrf", null, null).body(), "$.token");
    assertEquals(
        200,
        externo(
                otro,
                "POST",
                "/api/auth/login",
                "{\"identificador\":\"" + target + "\",\"contrasena\":\"" + password + "\"}",
                token)
            .statusCode());
    long version = directorio.cargar(target).version();
    assertEquals(200, editarHttp(target, "recepcion.actualizada", false, version).statusCode());
    assertEquals(401, externo(otro, "GET", "/api/auth/me", null, null).statusCode());
    assertEquals(
        200,
        editarHttp(target, "recepcion.actualizada", true, directorio.cargar(target).version())
            .statusCode());
    assertEquals(401, externo(otro, "GET", "/api/auth/me", null, null).statusCode());
    assertEquals(2, historialCuentas.historialCuenta(target, 0).elementos().size());
    token = read(externo(otro, "GET", "/api/auth/csrf", null, null).body(), "$.token");
    assertEquals(
        200,
        externo(
                otro,
                "POST",
                "/api/auth/login",
                "{\"identificador\":\"recepcion.actualizada\",\"contrasena\":\"" + password + "\"}",
                token)
            .statusCode());
  }

  @Test
  void unicidadYVersionEvitanDuplicadosYSobrescrituras() throws Exception {
    var duplicado = crearHttp(target.toString(), password, "[]");
    assertEquals(409, duplicado.statusCode());
    assertEquals("DUPLICADO", read(duplicado.body(), "$.code"));
    long version = directorio.cargar(target).version();
    assertEquals(409, editarHttp(target, actor.toString(), true, version).statusCode());
    assertEquals(target.toString(), directorio.cargar(target).identificador());
    assertEquals(200, editarHttp(target, "nombre.nuevo", true, version).statusCode());
    assertEquals(409, put(target, "[\"CAJ\"]", version).statusCode());
    long nueva = directorio.cargar(target).version();
    assertEquals(200, put(target, "[\"CAJ\"]", nueva).statusCode());
    assertEquals(409, editarHttp(target, "obsoleto", true, nueva).statusCode());
    assertEquals(1, historialCuentas.historialCuenta(target, 0).elementos().size());
  }

  @Test
  void altasConcurrentesDeMismoIdentificadorSoloCreanUnaCuenta() throws Exception {
    String nombre = "unica." + UUID.randomUUID();
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<String> alta =
          () -> {
            inicio.await();
            try {
              gestionar.crear(actor, nombre, password, Set.of(Rol.REC));
              return "OK";
            } catch (ErrorRoles e) {
              return e.codigo().name();
            }
          };
      var a = pool.submit(alta);
      var b = pool.submit(alta);
      inicio.countDown();
      assertEquals(
          Set.of("OK", "DUPLICADO"),
          Set.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)));
    }
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT count(*) FROM cuentas WHERE identificador=?", Integer.class, nombre));
    assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM auditoria_cuentas", Integer.class));
  }

  @Test
  void desactivarYRetirarRolCompartenProteccionDelUltimoAdministrador() throws Exception {
    assertEquals(
        409,
        editarHttp(actor, actor.toString(), false, directorio.cargar(actor).version())
            .statusCode());
    UUID segundo = crear(Rol.ADM);
    long va = directorio.cargar(actor).version(), vb = directorio.cargar(segundo).version();
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var a =
          pool.submit(
              () -> {
                inicio.await();
                try {
                  gestionar.actualizar(actor, actor, actor.toString(), false, va);
                  return "OK";
                } catch (ErrorRoles e) {
                  return e.codigo().name();
                }
              });
      var b = pool.submit(() -> retirar(inicio, segundo, vb));
      inicio.countDown();
      assertEquals(
          Set.of("OK", "ULTIMO_ADMIN"),
          Set.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)));
    }
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT count(*) FROM cuentas c JOIN cuenta_roles r ON r.cuenta_id=c.id WHERE c.activa AND r.rol='ADM'",
            Integer.class));
  }

  @Test
  void altasYEdicionesReviertenSiFallaAuditoria() throws Exception {
    String nombre = "fallo." + UUID.randomUUID();
    var antes = directorio.cargar(target);
    jdbc.execute(
        "ALTER TABLE auditoria_cuentas ADD CONSTRAINT simular_fallo CHECK(false) NOT VALID");
    try {
      assertEquals(503, crearHttp(nombre, password, "[\"CAJ\"]").statusCode());
      assertEquals(
          0,
          jdbc.queryForObject(
              "SELECT count(*) FROM cuentas WHERE identificador=?", Integer.class, nombre));
      assertEquals(503, editarHttp(target, "fallo.edicion", false, antes.version()).statusCode());
      assertEquals(antes, directorio.cargar(target));
    } finally {
      jdbc.execute("ALTER TABLE auditoria_cuentas DROP CONSTRAINT simular_fallo");
    }
  }

  @Test
  void administracionRechazaDatosInvalidosCsrfYPermisosRevocados() throws Exception {
    assertEquals(400, crearHttp("valida", "corta", "[]").statusCode());
    assertEquals(400, crearHttp("con espacios", password, "[]").statusCode());
    assertEquals(400, crearHttp("valida", password, "[\"ROOT\"]").statusCode());
    assertEquals(403, send("POST", "/api/cuentas", "{}", false).statusCode());
    assertEquals(403, send("PUT", "/api/cuentas/" + target, "{}", false).statusCode());
    jdbc.update("DELETE FROM cuenta_roles WHERE cuenta_id=?", actor);
    assertEquals(403, crearHttp("valida", password, "[]").statusCode());
    assertEquals(403, editarHttp(target, target.toString(), false, 1).statusCode());
    assertEquals(
        403, send("GET", "/api/cuentas/" + target + "/auditoria", null, false).statusCode());
    assertEquals(
        ErrorRoles.Codigo.PROHIBIDO,
        assertThrows(
                ErrorRoles.class,
                () -> gestionar.actualizar(actor, target, target.toString(), false, 1))
            .codigo());
  }

  UUID ficha() throws Exception {
    var respuesta =
        send(
            "POST",
            "/api/profesionales/fichas",
            "{\"identificacion\":\""
                + UUID.randomUUID()
                + "\",\"nombre\":\"Profesional de prueba\",\"funcion\":\"Medicina general\"}",
            true);
    assertEquals(201, respuesta.statusCode(), respuesta.body());
    return UUID.fromString(read(respuesta.body(), "$.id"));
  }

  HttpResponse<String> vincularHttp(UUID cuenta, UUID ficha, long version) throws Exception {
    String valor = ficha == null ? "null" : "\"" + ficha + "\"";
    return send(
        "PUT",
        "/api/cuentas/" + cuenta + "/profesional",
        "{\"profesionalId\":" + valor + ",\"version\":" + version + "}",
        true);
  }

  @Test
  void vinculaReemplazaYDesvinculaConAuditoria() throws Exception {
    UUID primera = ficha(), segunda = ficha();
    UUID cuenta = crear(Rol.PRO);
    long version = directorio.cargar(cuenta).version();
    assertEquals(200, vincularHttp(cuenta, primera, version).statusCode());
    assertEquals(primera, directorio.cargar(cuenta).profesionalId());
    assertEquals(409, vincularHttp(cuenta, segunda, version).statusCode());
    version = directorio.cargar(cuenta).version();
    assertEquals(200, vincularHttp(cuenta, primera, version).statusCode());
    assertEquals(1, historialCuentas.historialCuenta(cuenta, 0).elementos().size());
    assertEquals(200, vincularHttp(cuenta, segunda, version).statusCode());
    var cambio = historialCuentas.historialCuenta(cuenta, 0).elementos().getFirst();
    assertEquals(primera, cambio.profesionalAnterior());
    assertEquals(segunda, cambio.profesionalNuevo());
    assertEquals(actor, cambio.actorId());
    assertEquals("VINCULACION", cambio.tipo());
    assertEquals(409, put(cuenta, "[]", directorio.cargar(cuenta).version()).statusCode());
    assertEquals(200, vincularHttp(cuenta, null, directorio.cargar(cuenta).version()).statusCode());
    assertNull(directorio.cargar(cuenta).profesionalId());
    assertEquals(200, put(cuenta, "[]", directorio.cargar(cuenta).version()).statusCode());
  }

  @Test
  void rechazaFichaInexistenteInactivaSinRolYReversionPorFallo() throws Exception {
    UUID ficha = ficha();
    assertEquals(
        409, vincularHttp(target, ficha, directorio.cargar(target).version()).statusCode());
    UUID cuenta = crear(Rol.PRO);
    long version = directorio.cargar(cuenta).version();
    assertEquals(409, vincularHttp(cuenta, UUID.randomUUID(), version).statusCode());
    jdbc.update("UPDATE profesionales SET activa=false WHERE id=?", ficha);
    assertEquals(409, vincularHttp(cuenta, ficha, version).statusCode());
    jdbc.update("UPDATE profesionales SET activa=true WHERE id=?", ficha);
    jdbc.execute(
        "ALTER TABLE auditoria_cuentas ADD CONSTRAINT simular_fallo CHECK(false) NOT VALID");
    try {
      assertEquals(503, vincularHttp(cuenta, ficha, version).statusCode());
      assertNull(directorio.cargar(cuenta).profesionalId());
      assertEquals(version, directorio.cargar(cuenta).version());
    } finally {
      jdbc.execute("ALTER TABLE auditoria_cuentas DROP CONSTRAINT simular_fallo");
    }
  }

  @Test
  void dosCuentasNoPuedenVincularLaMismaFichaConcurrentemente() throws Exception {
    UUID ficha = ficha(), primera = crear(Rol.PRO), segunda = crear(Rol.PRO);
    long va = directorio.cargar(primera).version(), vb = directorio.cargar(segunda).version();
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(2)) {
      var a = pool.submit(() -> intentarVinculo(inicio, primera, ficha, va));
      var b = pool.submit(() -> intentarVinculo(inicio, segunda, ficha, vb));
      inicio.countDown();
      assertEquals(
          Set.of("OK", "VINCULO_OCUPADO"),
          Set.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)));
    }
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT count(*) FROM cuentas WHERE profesional_id=?", Integer.class, ficha));
    assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM auditoria_cuentas", Integer.class));
  }

  String intentarVinculo(CountDownLatch inicio, UUID cuenta, UUID ficha, long version)
      throws Exception {
    inicio.await();
    try {
      gestionar.vincular(actor, cuenta, ficha, version);
      return "OK";
    } catch (ErrorRoles error) {
      return error.codigo().name();
    }
  }

  @Test
  void fichasYVinculosValidanPermisosCsrfYUnicidad() throws Exception {
    UUID ficha = ficha();
    String identificacion =
        jdbc.queryForObject(
            "SELECT identificacion FROM profesionales WHERE id=?", String.class, ficha);
    assertEquals(
        409,
        send(
                "POST",
                "/api/profesionales/fichas",
                "{\"identificacion\":\""
                    + identificacion
                    + "\",\"nombre\":\"Otra ficha\",\"funcion\":\"Medicina\"}",
                true)
            .statusCode());
    assertEquals(400, send("POST", "/api/profesionales/fichas", "{}", true).statusCode());
    assertEquals(
        200,
        send("GET", "/api/profesionales/fichas?busqueda=" + identificacion, null, false)
            .statusCode());
    assertEquals(
        404,
        send("GET", "/api/profesionales/fichas/" + UUID.randomUUID(), null, false).statusCode());
    assertEquals(403, send("POST", "/api/profesionales/fichas", "{}", false).statusCode());
    assertEquals(
        403, send("PUT", "/api/cuentas/" + target + "/profesional", "{}", false).statusCode());
    jdbc.update("DELETE FROM cuenta_roles WHERE cuenta_id=?", actor);
    assertEquals(403, send("GET", "/api/profesionales/fichas", null, false).statusCode());
    assertEquals(403, send("POST", "/api/profesionales/fichas", "{}", true).statusCode());
    assertEquals(403, vincularHttp(target, ficha, 1).statusCode());
  }
}
