package pe.centrosalud.integration;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.time.LocalDate;
import java.time.LocalTime;
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
import pe.centrosalud.modules.administration.application.ports.Disponibilidad;
import pe.centrosalud.modules.administration.application.usecases.ErrorDisponibilidad;
import pe.centrosalud.modules.administration.application.usecases.GestionarDisponibilidad;
import pe.centrosalud.modules.administration.application.usecases.GestionarProfesionales;
import pe.centrosalud.modules.administration.domain.TipoRecurso;
import pe.centrosalud.modules.administration.domain.TipoTurno;
import pe.centrosalud.modules.identity.application.ports.HashContrasenas;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.servlet.session.cookie.secure=false")
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class DisponibilidadHttpTest {
  static final String SCHEMA =
      "test_disponibilidad_" + UUID.randomUUID().toString().replace("-", "");

  @DynamicPropertySource
  static void schema(DynamicPropertyRegistry registry) {
    registry.add("spring.flyway.default-schema", () -> SCHEMA);
    registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
  }

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  @Autowired HashContrasenas hashes;
  @Autowired GestionarDisponibilidad gestionar;
  @Autowired GestionarProfesionales gestionarProfesionales;
  @Autowired Disponibilidad disponibilidad;
  String password, csrf;
  HttpClient client;
  UUID profesionalId;

  @BeforeEach
  void preparar() {
    password = UUID.randomUUID().toString();
    profesionalId =
        gestionarProfesionales
            .crear(UUID.randomUUID(), "P-" + UUID.randomUUID(), "Profesional", "Medicina", Set.of())
            .id();
  }

  @AfterEach
  void limpiar() {
    jdbc.execute(
        "TRUNCATE auditoria_turnos,turnos_profesional,turnos_consultorio,profesional_servicios,auditoria_profesionales,profesionales,spring_session,intentos_login,cuentas CASCADE");
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

  String ruta(UUID profesionalId) {
    return "/api/disponibilidad/profesionales/" + profesionalId;
  }

  String nuevoTurno(String inicio, String fin, String tipo) {
    return "{\"fecha\":\"2026-10-15\",\"horaInicio\":\""
        + inicio
        + "\",\"horaFin\":\""
        + fin
        + "\",\"tipo\":\""
        + tipo
        + "\"}";
  }

  HttpResponse<String> crear(UUID profesionalId, String inicio, String fin) throws Exception {
    return send("POST", ruta(profesionalId), nuevoTurno(inicio, fin, "DISPONIBLE"), true);
  }

  int contar(String tabla) {
    return jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
  }

  @Test
  void administradorCreaListaYAuditaElAlta() throws Exception {
    UUID adm = ingresar("ADM");
    var respuesta = crear(profesionalId, "08:00:00", "12:00:00");
    assertEquals(201, respuesta.statusCode(), respuesta.body());
    UUID turnoId = UUID.fromString(read(respuesta.body(), "$.id"));
    assertEquals("DISPONIBLE", read(respuesta.body(), "$.tipo"));
    var listado = send("GET", ruta(profesionalId), null, false);
    assertEquals(200, listado.statusCode());
    assertEquals(1, ((List<?>) read(listado.body(), "$")).size());
    var auditoria =
        jdbc.queryForMap(
            "SELECT actor_id,tipo_recurso,tipo FROM auditoria_turnos WHERE turno_id=?", turnoId);
    assertEquals(adm, auditoria.get("actor_id"));
    assertEquals("PROFESIONAL", auditoria.get("tipo_recurso"));
    assertEquals("CREACION", auditoria.get("tipo"));
  }

  @Test
  void turnosAdyacentesSeAceptanYLosQueSeCruzanSeRechazan() throws Exception {
    ingresar("ADM");
    assertEquals(201, crear(profesionalId, "08:00:00", "12:00:00").statusCode());
    // Adyacente: termina cuando empieza el siguiente.
    assertEquals(
        201,
        send("POST", ruta(profesionalId), nuevoTurno("12:00:00", "13:00:00", "DESCANSO"), true)
            .statusCode());
    assertEquals(201, crear(profesionalId, "13:00:00", "17:00:00").statusCode());
    // Se cruza con el turno de la tarde.
    var superpuesto = crear(profesionalId, "16:45:00", "18:00:00");
    assertEquals(409, superpuesto.statusCode());
    assertEquals("SUPERPUESTO", read(superpuesto.body(), "$.code"));
    assertEquals(3, contar("turnos_profesional"));
  }

  @Test
  void intervaloInvalidoYRecursoInexistenteSeRechazan() throws Exception {
    ingresar("ADM");
    assertEquals(400, crear(profesionalId, "12:00:00", "08:00:00").statusCode());
    var inexistente =
        send(
            "POST",
            ruta(UUID.randomUUID()),
            nuevoTurno("08:00:00", "12:00:00", "DISPONIBLE"),
            true);
    assertEquals(400, inexistente.statusCode());
    assertEquals("RECURSO_INVALIDO", read(inexistente.body(), "$.code"));
    assertEquals(0, contar("turnos_profesional"));
  }

  @Test
  void desactivarEsIdempotenteYNoAuditaDosVeces() throws Exception {
    ingresar("ADM");
    UUID turnoId =
        UUID.fromString(read(crear(profesionalId, "08:00:00", "12:00:00").body(), "$.id"));
    assertEquals(
        200,
        send("PUT", ruta(profesionalId) + "/" + turnoId, "{\"activo\":false}", true).statusCode());
    assertFalse(
        jdbc.queryForObject(
            "SELECT activo FROM turnos_profesional WHERE id=?", Boolean.class, turnoId));
    assertEquals(2, contar("auditoria_turnos"));
    assertEquals(
        200,
        send("PUT", ruta(profesionalId) + "/" + turnoId, "{\"activo\":false}", true).statusCode());
    assertEquals(2, contar("auditoria_turnos"));
    // Un turno inactivo libera el horario para uno nuevo.
    assertEquals(201, crear(profesionalId, "08:00:00", "12:00:00").statusCode());
  }

  @Test
  void desactivarUnTurnoInexistenteRecibe404() throws Exception {
    ingresar("ADM");
    var respuesta =
        send("PUT", ruta(profesionalId) + "/" + UUID.randomUUID(), "{\"activo\":false}", true);
    assertEquals(404, respuesta.statusCode());
    assertEquals("NO_EXISTE", read(respuesta.body(), "$.code"));
  }

  @Test
  void diezAltasConcurrentesSuperpuestasSoloCreanUnTurno() throws Exception {
    UUID adm = cuenta("ADM");
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(10)) {
      List<Future<String>> tareas = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        int offset = i;
        tareas.add(
            pool.submit(
                () -> {
                  inicio.await();
                  var horaInicio = LocalTime.of(8, 0).plusMinutes(offset);
                  try {
                    gestionar.crear(
                        adm,
                        TipoRecurso.PROFESIONAL,
                        profesionalId,
                        LocalDate.of(2026, 10, 15),
                        horaInicio,
                        horaInicio.plusHours(1),
                        TipoTurno.DISPONIBLE);
                    return "OK";
                  } catch (ErrorDisponibilidad e) {
                    return e.codigo().name();
                  }
                }));
      }
      inicio.countDown();
      List<String> resultados = new ArrayList<>();
      for (var tarea : tareas) resultados.add(tarea.get(15, TimeUnit.SECONDS));
      assertEquals(1, Collections.frequency(resultados, "OK"), resultados.toString());
      assertEquals(9, Collections.frequency(resultados, "SUPERPUESTO"), resultados.toString());
    }
    assertEquals(1, contar("turnos_profesional"));
  }

  @Test
  void diezAltasConcurrentesDisjuntasSeCreanTodas() throws Exception {
    UUID adm = cuenta("ADM");
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(10)) {
      List<Future<String>> tareas = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        int hora = 8 + i;
        tareas.add(
            pool.submit(
                () -> {
                  inicio.await();
                  gestionar.crear(
                      adm,
                      TipoRecurso.PROFESIONAL,
                      profesionalId,
                      LocalDate.of(2026, 10, 15),
                      LocalTime.of(hora, 0),
                      LocalTime.of(hora + 1, 0),
                      TipoTurno.DISPONIBLE);
                  return "OK";
                }));
      }
      inicio.countDown();
      for (var tarea : tareas) assertEquals("OK", tarea.get(15, TimeUnit.SECONDS));
    }
    assertEquals(10, contar("turnos_profesional"));
  }

  @Test
  void soloAdministracionGestionaDisponibilidad() throws Exception {
    UUID adm = cuenta("ADM");
    gestionar.crear(
        adm,
        TipoRecurso.PROFESIONAL,
        profesionalId,
        LocalDate.of(2026, 10, 15),
        LocalTime.of(8, 0),
        LocalTime.of(12, 0),
        TipoTurno.DISPONIBLE);
    for (String rol : List.of("REC", "CAJ", "PRO", "")) {
      if (rol.isEmpty()) ingresar();
      else ingresar(rol);
      assertEquals(403, send("GET", ruta(profesionalId), null, false).statusCode(), rol);
      assertEquals(403, crear(profesionalId, "13:00:00", "17:00:00").statusCode(), rol);
    }
    assertEquals(1, contar("turnos_profesional"));
    ingresar("ADM", "REC");
    assertEquals(201, crear(profesionalId, "13:00:00", "17:00:00").statusCode());
  }

  @Test
  void exigeSesionCsrfYPermisosVigentes() throws Exception {
    client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    token();
    assertEquals(401, send("GET", ruta(profesionalId), null, false).statusCode());
    assertEquals(401, crear(profesionalId, "08:00:00", "12:00:00").statusCode());
    UUID adm = ingresar("ADM");
    assertEquals(
        403,
        send("POST", ruta(profesionalId), nuevoTurno("08:00:00", "12:00:00", "DISPONIBLE"), false)
            .statusCode());
    jdbc.update("DELETE FROM cuenta_roles WHERE cuenta_id=?", adm);
    assertEquals(403, crear(profesionalId, "08:00:00", "12:00:00").statusCode());
    UUID otra = ingresar("ADM");
    jdbc.update("UPDATE cuentas SET activa=false WHERE id=?", otra);
    assertEquals(401, crear(profesionalId, "08:00:00", "12:00:00").statusCode());
    assertEquals(0, contar("turnos_profesional"));
  }

  @Test
  void elTurnoDebePertenecerAlRecursoDeLaRuta() throws Exception {
    UUID actor = ingresar("ADM");
    var turno =
        gestionar.crear(
            actor,
            TipoRecurso.PROFESIONAL,
            profesionalId,
            LocalDate.now().plusDays(1),
            LocalTime.of(8, 0),
            LocalTime.of(9, 0),
            TipoTurno.DISPONIBLE);
    var otro =
        gestionarProfesionales
            .crear(actor, "OTRO-" + UUID.randomUUID(), "Otro profesional", "Medicina", Set.of())
            .id();
    var r =
        send(
            "PUT",
            "/api/disponibilidad/profesionales/" + otro + "/" + turno.id(),
            "{\"activo\":false}",
            true);
    assertEquals(404, r.statusCode(), r.body());
    assertTrue(disponibilidad.listar(TipoRecurso.PROFESIONAL, profesionalId).getFirst().activo());
  }
}
