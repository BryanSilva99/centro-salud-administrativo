package pe.centrosalud.integration;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import pe.centrosalud.modules.identity.application.ports.HashContrasenas;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.servlet.session.cookie.secure=false")
@Import(CitasIntegradasHttpTest.Tiempo.class)
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class CitasIntegradasHttpTest {
  static final String SCHEMA =
      "test_citas_integradas_" + UUID.randomUUID().toString().replace("-", "");

  @DynamicPropertySource
  static void schema(DynamicPropertyRegistry r) {
    r.add("spring.flyway.default-schema", () -> SCHEMA);
    r.add("spring.datasource.hikari.schema", () -> SCHEMA);
  }

  @TestConfiguration
  static class Tiempo {
    @Bean
    @Primary
    Clock relojCitas() {
      return Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneId.of("America/Lima"));
    }
  }

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  @Autowired HashContrasenas hashes;
  UUID paciente,
      profesional,
      otroProfesional,
      servicio,
      consultorio,
      modalidad,
      rec,
      pro,
      otroPro,
      adm,
      caj;
  String password;

  record Sesion(HttpClient client, String csrf) {}

  @BeforeEach
  void preparar() {
    paciente = UUID.randomUUID();
    profesional = UUID.randomUUID();
    otroProfesional = UUID.randomUUID();
    servicio = UUID.randomUUID();
    consultorio = UUID.randomUUID();
    modalidad = UUID.randomUUID();
    password = UUID.randomUUID().toString();
    for (var p : List.of(profesional, otroProfesional))
      jdbc.update(
          "INSERT INTO profesionales(id,identificacion,nombre,funcion,creado_por) VALUES (?,?,?,'Medicina',?)",
          p,
          p.toString(),
          "Profesional sintético",
          p);
    jdbc.update(
        "INSERT INTO pacientes(id,tipo_documento,numero_documento,nombres,apellidos,fecha_nacimiento,creado_por) VALUES (?,'DNI','12345678','Paciente','Sintético','2000-01-01',?)",
        paciente,
        profesional);
    jdbc.update(
        "INSERT INTO servicios(id,nombre,duracion_minutos,creado_por) VALUES (?,'Consulta',30,?)",
        servicio,
        profesional);
    jdbc.update(
        "INSERT INTO servicio_modalidades(id,servicio_id,nombre,con_cargo,tarifa) VALUES (?,?,'Particular',true,40)",
        modalidad,
        servicio);
    jdbc.update(
        "INSERT INTO consultorios(id,codigo,nombre,creado_por) VALUES (?,'C1','Consultorio',?)",
        consultorio,
        profesional);
    jdbc.update("INSERT INTO profesional_servicios VALUES (?,?)", profesional, servicio);
    jdbc.update("INSERT INTO consultorio_servicios VALUES (?,?)", consultorio, servicio);
    jdbc.update(
        "INSERT INTO turnos_profesional(id,profesional_id,fecha,hora_inicio,hora_fin,tipo,creado_por) VALUES (?,?,'2026-10-12','08:07','12:00','DISPONIBLE',?)",
        UUID.randomUUID(),
        profesional,
        profesional);
    jdbc.update(
        "INSERT INTO turnos_consultorio(id,consultorio_id,fecha,hora_inicio,hora_fin,tipo,creado_por) VALUES (?,?,'2026-10-12','09:00','11:00','DISPONIBLE',?)",
        UUID.randomUUID(),
        consultorio,
        profesional);
    rec = cuenta("REC", null);
    pro = cuenta("PRO", profesional);
    otroPro = cuenta("PRO", otroProfesional);
    adm = cuenta("ADM", null);
    caj = cuenta("CAJ", null);
  }

  UUID cuenta(String rol, UUID ficha) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO cuentas(id,identificador,activa,contrasena_hash,profesional_id) VALUES (?,?,true,?,?)",
        id,
        id.toString(),
        hashes.generar(password),
        ficha);
    jdbc.update("INSERT INTO cuenta_roles VALUES (?,?)", id, rol);
    return id;
  }

  @AfterEach
  void limpiar() {
    jdbc.execute(
        "TRUNCATE citas,reprogramaciones_cita,cuentas,pacientes,profesionales,servicios,consultorios,spring_session,intentos_login CASCADE");
  }

  @AfterAll
  static void cerrar(@Autowired JdbcTemplate jdbc) {
    jdbc.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
  }

  HttpResponse<String> enviar(Sesion s, String method, String path, String body, boolean csrf)
      throws Exception {
    var b =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .header("Content-Type", "application/json");
    if (csrf) b.header("X-CSRF-TOKEN", s.csrf());
    return s.client()
        .send(
            b.method(
                    method,
                    body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  Sesion login(UUID id) throws Exception {
    var c =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    var s =
        new Sesion(
            c,
            read(
                enviar(new Sesion(c, ""), "GET", "/api/auth/csrf", null, false).body(), "$.token"));
    assertEquals(
        200,
        enviar(
                s,
                "POST",
                "/api/auth/login",
                "{\"identificador\":\"" + id + "\",\"contrasena\":\"" + password + "\"}",
                true)
            .statusCode());
    return new Sesion(c, read(enviar(s, "GET", "/api/auth/csrf", null, false).body(), "$.token"));
  }

  String reserva(String hora) {
    return """
      {"pacienteId":"%s","profesionalId":"%s","servicioId":"%s","consultorioId":"%s","modalidad":"%s","fecha":"2026-10-12","hora":"%s"}
      """
        .formatted(paciente, profesional, servicio, consultorio, modalidad, hora);
  }

  @Test
  void recorridoCompletoConDatosRealesYPermisos() throws Exception {
    var r = login(rec);
    var p = login(pro);
    var ajeno = login(otroPro);
    var creada = enviar(r, "POST", "/api/citas", reserva("09:00"), true);
    assertEquals(201, creada.statusCode(), creada.body());
    String id = read(creada.body(), "$.id");
    assertEquals(40, ((Number) read(creada.body(), "$.importe")).intValue());
    assertEquals(
        200,
        enviar(
                r,
                "POST",
                "/api/citas/" + id + "/reprogramaciones",
                "{\"fecha\":\"2026-10-12\",\"hora\":\"10:00\",\"motivo\":\"Solicitud\",\"version\":0}",
                true)
            .statusCode());
    assertEquals(
        409,
        enviar(
                r,
                "POST",
                "/api/citas/" + id + "/reprogramaciones",
                "{\"fecha\":\"2026-10-12\",\"hora\":\"09:00\",\"motivo\":\"Solicitud\",\"version\":0}",
                true)
            .statusCode());
    assertEquals(
        200,
        enviar(r, "PUT", "/api/citas/" + id + "/llegada", "{\"version\":1}", true).statusCode());
    assertEquals(
        403,
        enviar(ajeno, "PUT", "/api/citas/" + id + "/atencion/inicio", "{\"version\":2}", true)
            .statusCode());
    assertEquals(
        403,
        enviar(r, "PUT", "/api/citas/" + id + "/atencion/inicio", "{\"version\":2}", true)
            .statusCode());
    assertEquals(
        200,
        enviar(p, "PUT", "/api/citas/" + id + "/atencion/inicio", "{\"version\":2}", true)
            .statusCode());
    var fin =
        enviar(p, "PUT", "/api/citas/" + id + "/atencion/finalizacion", "{\"version\":3}", true);
    assertEquals(200, fin.statusCode(), fin.body());
    assertEquals("ATENDIDA", read(fin.body(), "$.estado"));
    assertEquals(
        1, jdbc.queryForObject("SELECT count(*) FROM reprogramaciones_cita", Integer.class));
  }

  @Test
  void contextoYAgendaRespetanAmbitoYRevocacion() throws Exception {
    var r = login(rec);
    assertEquals(201, enviar(r, "POST", "/api/citas", reserva("09:00"), true).statusCode());
    var a = login(adm);
    var p = login(pro);
    var ajeno = login(otroPro);
    assertEquals(
        1,
        ((List<?>) read(enviar(r, "GET", "/api/citas/contexto", null, false).body(), "$.pacientes"))
            .size());
    for (var sesion : List.of(a, p, ajeno))
      assertTrue(
          ((List<?>)
                  read(
                      enviar(sesion, "GET", "/api/citas/contexto", null, false).body(),
                      "$.pacientes"))
              .isEmpty());
    assertEquals(
        200, enviar(a, "GET", "/api/citas/agenda?fecha=2026-10-12", null, false).statusCode());
    assertEquals(403, enviar(a, "POST", "/api/citas", reserva("10:00"), true).statusCode());
    assertEquals(
        "[]",
        enviar(
                ajeno,
                "GET",
                "/api/citas/agenda?fecha=2026-10-12&profesionalId=" + profesional,
                null,
                false)
            .body());
    assertEquals(403, enviar(login(caj), "GET", "/api/citas/contexto", null, false).statusCode());
    assertEquals(403, enviar(r, "POST", "/api/citas", reserva("10:00"), false).statusCode());
    jdbc.update("DELETE FROM cuenta_roles WHERE cuenta_id=?", rec);
    assertEquals(403, enviar(r, "GET", "/api/citas/contexto", null, false).statusCode());
  }

  @Test
  void disponibilidadExigeHabilitacionTurnosYRecursosActivos() throws Exception {
    var r = login(rec);
    assertEquals(422, enviar(r, "POST", "/api/citas", reserva("08:15"), true).statusCode());
    assertEquals(422, enviar(r, "POST", "/api/citas", reserva("10:45"), true).statusCode());
    jdbc.update("DELETE FROM profesional_servicios");
    assertEquals(422, enviar(r, "POST", "/api/citas", reserva("09:00"), true).statusCode());
    jdbc.update("INSERT INTO profesional_servicios VALUES (?,?)", profesional, servicio);
    jdbc.update("UPDATE consultorios SET activo=false");
    assertEquals(404, enviar(r, "POST", "/api/citas", reserva("09:00"), true).statusCode());
  }

  @Test
  void reservasConcurrentesSoloConfirmanUnaYCancelacionLiberaHorario() throws Exception {
    var r = login(rec);
    try (var executor = Executors.newFixedThreadPool(6)) {
      var inicio = new CountDownLatch(1);
      var tareas = new ArrayList<Future<HttpResponse<String>>>();
      for (int i = 0; i < 6; i++)
        tareas.add(
            executor.submit(
                () -> {
                  inicio.await();
                  return enviar(r, "POST", "/api/citas", reserva("09:00"), true);
                }));
      inicio.countDown();
      int creadas = 0;
      String id = null;
      for (var f : tareas) {
        var respuesta = f.get(30, TimeUnit.SECONDS);
        if (respuesta.statusCode() == 201) {
          creadas++;
          id = read(respuesta.body(), "$.id");
        } else assertEquals(409, respuesta.statusCode(), respuesta.body());
      }
      assertEquals(1, creadas);
      var cancelada =
          enviar(
              r,
              "PUT",
              "/api/citas/" + id + "/cancelacion",
              "{\"motivo\":\"Solicitud\",\"version\":0}",
              true);
      assertEquals(200, cancelada.statusCode(), cancelada.body());
      assertEquals(false, read(cancelada.body(), "$.gestionEconomicaPendiente"));
      assertEquals(201, enviar(r, "POST", "/api/citas", reserva("09:00"), true).statusCode());
    }
  }

  @Test
  void horariosOfrecidosSeAlineanYUnaModalidadInactivaNoSeReserva() throws Exception {
    var r = login(rec);
    jdbc.update("UPDATE turnos_consultorio SET hora_inicio='08:00'");
    var ruta =
        "/api/citas/disponibilidad?fecha=2026-10-12&pacienteId="
            + paciente
            + "&profesionalId="
            + profesional
            + "&servicioId="
            + servicio
            + "&consultorioId="
            + consultorio;
    var disponibles = enviar(r, "GET", ruta, null, false);
    assertEquals(200, disponibles.statusCode(), disponibles.body());
    assertEquals(
        OffsetDateTime.parse("2026-10-12T08:15:00-05:00"),
        OffsetDateTime.parse(read(disponibles.body(), "$[0].inicio")));
    jdbc.update(
        "INSERT INTO servicio_modalidades(id,servicio_id,nombre,con_cargo,tarifa) VALUES (?,?,'Otra',false,0)",
        UUID.randomUUID(),
        servicio);
    jdbc.update("UPDATE servicio_modalidades SET activa=false WHERE id=?", modalidad);
    assertEquals(404, enviar(r, "POST", "/api/citas", reserva("09:00"), true).statusCode());
  }

  @Test
  void cambiosDeRecursosImpidenIniciarAtencionYRetiroConservaLlegada() throws Exception {
    var r = login(rec);
    var p = login(pro);
    String id = read(enviar(r, "POST", "/api/citas", reserva("09:00"), true).body(), "$.id");
    assertEquals(
        200,
        enviar(r, "PUT", "/api/citas/" + id + "/llegada", "{\"version\":0}", true).statusCode());
    jdbc.update("UPDATE turnos_consultorio SET activo=false");
    assertEquals(
        422,
        enviar(p, "PUT", "/api/citas/" + id + "/atencion/inicio", "{\"version\":1}", true)
            .statusCode());
    var retirada =
        enviar(
            r,
            "PUT",
            "/api/citas/" + id + "/espera/retiro",
            "{\"motivo\":\"Retiro solicitado\",\"version\":1}",
            true);
    assertEquals(200, retirada.statusCode(), retirada.body());
    assertNotNull(read(retirada.body(), "$.cita.llegadaEn"));
    assertEquals("RETIRO_ESPERA", read(retirada.body(), "$.cita.tipoCierre"));
  }
}
