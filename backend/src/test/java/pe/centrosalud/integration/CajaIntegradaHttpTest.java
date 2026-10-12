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
@Import(CajaIntegradaHttpTest.Tiempo.class)
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class CajaIntegradaHttpTest {
  static final String SCHEMA =
      "test_caja_integrada_" + UUID.randomUUID().toString().replace("-", "");

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
    jdbc.execute("drop trigger if exists fallo on pagos");
    jdbc.execute("drop function if exists fallar_pago()");
    jdbc.execute(
        "TRUNCATE cargos,sesiones_caja,solicitudes_reversion,reintentos_caja,compensaciones_caja,citas,reprogramaciones_cita,cuentas,pacientes,profesionales,servicios,consultorios,spring_session,intentos_login CASCADE");
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

  HttpResponse<String> post(Sesion s, String path, String body) throws Exception {
    return enviar(s, "POST", "/api/billing" + path, body, true);
  }

  String id(HttpResponse<String> r) {
    assertEquals(200, r.statusCode(), r.body());
    return read(r.body(), "$.id");
  }

  String cita() throws Exception {
    var r = enviar(login(rec), "POST", "/api/citas", reserva("09:00"), true);
    assertEquals(201, r.statusCode(), r.body());
    return read(r.body(), "$.id");
  }

  String cargo(Sesion s, String cita) throws Exception {
    return id(
        post(
            s,
            "/cargos",
            "{\"citaId\":\""
                + cita
                + "\",\"importe\":1,\"pacienteId\":\""
                + UUID.randomUUID()
                + "\"}"));
  }

  String abrir(Sesion s) throws Exception {
    return id(
        post(s, "/sesiones", "{\"fondoInicial\":100,\"clave\":\"" + UUID.randomUUID() + "\"}"));
  }

  String pagoBody(String cargo, String sesion, String importe, UUID clave) {
    return """
   {"cargoId":"%s","sesionCajaId":"%s","importe":%s,"medio":"EFECTIVO","clave":"%s"}
   """
        .formatted(cargo, sesion, importe, clave);
  }

  double numero(String json, String path) {
    return ((Number) read(json, path)).doubleValue();
  }

  HttpResponse<String> saldo(Sesion s, String cargo) throws Exception {
    return enviar(s, "GET", "/api/billing/cargos/" + cargo + "/saldo", null, false);
  }

  String solicitar(Sesion s, String original, String tipo) throws Exception {
    return id(
        post(
            s,
            "/reversiones",
            "{\"operacionId\":\""
                + original
                + "\",\"tipo\":\""
                + tipo
                + "\",\"motivo\":\"Devolución solicitada por paciente\"}"));
  }

  @Test
  void recorridoPagoReversionCierreConservaTarifaYActor() throws Exception {
    var c = login(caj);
    var a = login(adm);
    String cita = cita();
    jdbc.update("update servicio_modalidades set tarifa=90 where id=?", modalidad);
    String cargo = cargo(c, cita);
    assertEquals(cargo, cargo(c, cita));
    assertEquals(40, numero(saldo(c, cargo).body(), "$.cargo.importe"));
    assertEquals(paciente.toString(), read(saldo(c, cargo).body(), "$.cargo.pacienteId"));
    String sesion = abrir(c);
    var clave = UUID.randomUUID();
    String body = pagoBody(cargo, sesion, "40", clave);
    String pago = id(post(c, "/pagos", body));
    assertEquals(pago, id(post(c, "/pagos", body)));
    assertEquals(0, numero(saldo(c, cargo).body(), "$.saldoPendiente"));
    assertEquals(cargo, cargo(c, cita)); // PAGADO sigue siendo cargo vigente.
    var cierre = post(c, "/sesiones/" + sesion + "/cerrar", "{\"efectivoDeclarado\":140}");
    assertEquals(200, cierre.statusCode(), cierre.body());
    String solicitud = solicitar(c, pago, "PAGO");
    assertEquals(
        409, post(a, "/reversiones/" + solicitud + "/resolver", "{\"aprobar\":true}").statusCode());
    String nueva = abrir(c);
    assertEquals(
        200, post(a, "/reversiones/" + solicitud + "/resolver", "{\"aprobar\":true}").statusCode());
    assertEquals(
        200, post(a, "/reversiones/" + solicitud + "/resolver", "{\"aprobar\":true}").statusCode());
    var saldo = saldo(c, cargo);
    assertEquals(40, numero(saldo.body(), "$.saldoPendiente"));
    assertEquals(2, ((List<?>) read(saldo.body(), "$.movimientos")).size());
    var anterior = enviar(a, "GET", "/api/billing/sesiones/" + sesion, null, false);
    assertEquals(140, numero(anterior.body(), "$.efectivoEsperado"));
    assertEquals(1, ((List<?>) read(anterior.body(), "$.movimientos")).size());
    var actual = enviar(c, "GET", "/api/billing/sesiones/" + nueva, null, false);
    assertEquals(60, numero(actual.body(), "$.efectivoEsperado"));
    assertEquals(
        409,
        post(
                c,
                "/reversiones",
                "{\"operacionId\":\"" + pago + "\",\"tipo\":\"PAGO\",\"motivo\":\"Otra\"}")
            .statusCode());
    String anular = solicitar(c, cargo, "CARGO");
    assertEquals(
        200, post(a, "/reversiones/" + anular + "/resolver", "{\"aprobar\":true}").statusCode());
    assertEquals(0, numero(saldo(c, cargo).body(), "$.saldoPendiente"));
    assertEquals("ANULADO", read(saldo(c, cargo).body(), "$.cargo.estado"));
  }

  @Test
  void permisosPropiedadSesionCsrfYRevocacion() throws Exception {
    var c = login(caj);
    var a = login(adm);
    var r = login(rec);
    var ajeno = login(cuenta("CAJ", null));
    String cita = cita();
    String cargo = cargo(c, cita);
    String sesion = abrir(c);
    assertEquals(
        403,
        post(a, "/sesiones", "{\"fondoInicial\":0,\"clave\":\"" + UUID.randomUUID() + "\"}")
            .statusCode());
    assertEquals(403, post(r, "/cargos", "{\"citaId\":\"" + cita + "\"}").statusCode());
    assertEquals(
        403, post(ajeno, "/pagos", pagoBody(cargo, sesion, "10", UUID.randomUUID())).statusCode());
    assertEquals(
        403,
        post(ajeno, "/sesiones/" + sesion + "/cerrar", "{\"efectivoDeclarado\":100}").statusCode());
    assertEquals(
        403, enviar(ajeno, "GET", "/api/billing/sesiones/" + sesion, null, false).statusCode());
    assertEquals(
        403,
        enviar(
                c,
                "POST",
                "/api/billing/pagos",
                pagoBody(cargo, sesion, "10", UUID.randomUUID()),
                false)
            .statusCode());
    String solicitud = solicitar(c, cargo, "CARGO");
    jdbc.update("insert into cuenta_roles values (?,'ADM')", caj);
    assertEquals(
        403,
        post(
                c,
                "/reversiones/" + solicitud + "/resolver",
                "{\"aprobar\":true,\"aprobadorId\":\"" + adm + "\"}")
            .statusCode());
    jdbc.update("delete from cuenta_roles where cuenta_id=?", caj);
    assertEquals(403, saldo(c, cargo).statusCode());
  }

  @Test
  void diezPagosConcurrentesSinSobrepagoYReintentoIdentificado() throws Exception {
    var c = login(caj);
    String cargo = cargo(c, cita());
    String sesion = abrir(c);
    try (var executor = Executors.newFixedThreadPool(10)) {
      var start = new CountDownLatch(1);
      var futures = new ArrayList<Future<HttpResponse<String>>>();
      for (int i = 0; i < 10; i++)
        futures.add(
            executor.submit(
                () -> {
                  start.await();
                  return post(c, "/pagos", pagoBody(cargo, sesion, "40", UUID.randomUUID()));
                }));
      start.countDown();
      int exito = 0;
      for (var f : futures) {
        var r = f.get(30, TimeUnit.SECONDS);
        if (r.statusCode() == 200) exito++;
        else assertEquals(400, r.statusCode(), r.body());
      }
      assertEquals(1, exito);
      assertEquals(0, numero(saldo(c, cargo).body(), "$.saldoPendiente"));
      assertEquals(1, jdbc.queryForObject("select count(*) from pagos", Integer.class));
    }
  }

  @Test
  void aperturaConcurrenteYReversionConcurrenteSoloUnaVez() throws Exception {
    var c = login(caj);
    var a = login(adm);
    String cargo = cargo(c, cita());
    try (var executor = Executors.newFixedThreadPool(10)) {
      var start = new CountDownLatch(1);
      var futures = new ArrayList<Future<HttpResponse<String>>>();
      for (int i = 0; i < 10; i++)
        futures.add(
            executor.submit(
                () -> {
                  start.await();
                  return post(
                      c,
                      "/sesiones",
                      "{\"fondoInicial\":0,\"clave\":\"" + UUID.randomUUID() + "\"}");
                }));
      start.countDown();
      int exito = 0;
      String sesion = null;
      for (var f : futures) {
        var res = f.get(30, TimeUnit.SECONDS);
        if (res.statusCode() == 200) {
          exito++;
          sesion = id(res);
        } else assertEquals(409, res.statusCode(), res.body());
      }
      assertEquals(1, exito);
      String pago = id(post(c, "/pagos", pagoBody(cargo, sesion, "40", UUID.randomUUID())));
      String solicitud = solicitar(c, pago, "PAGO");
      futures.clear();
      var barrera = new CountDownLatch(1);
      for (int i = 0; i < 10; i++)
        futures.add(
            executor.submit(
                () -> {
                  barrera.await();
                  return post(a, "/reversiones/" + solicitud + "/resolver", "{\"aprobar\":true}");
                }));
      barrera.countDown();
      for (var f : futures) {
        var res = f.get(30, TimeUnit.SECONDS);
        assertEquals(200, res.statusCode(), res.body());
      }
      assertEquals(
          1,
          jdbc.queryForObject("select count(*) from pagos where tipo='DEVOLUCION'", Integer.class));
      assertEquals(40, numero(saldo(c, cargo).body(), "$.saldoPendiente"));
    }
  }

  @Test
  void cierreConcurrenteNoRecibeMovimientosPosterioresYRollback() throws Exception {
    var c = login(caj);
    String cargo = cargo(c, cita());
    String sesion = abrir(c);
    // Fallo en persistencia después de actualizar el cargo: la transacción debe deshacerlo todo.
    jdbc.execute(
        "create function fallar_pago() returns trigger language plpgsql as $$ begin raise exception 'fallo sintético'; end $$");
    jdbc.execute(
        "create trigger fallo before insert on pagos for each row execute function fallar_pago()");
    assertEquals(
        500, post(c, "/pagos", pagoBody(cargo, sesion, "40", UUID.randomUUID())).statusCode());
    assertEquals("PENDIENTE", read(saldo(c, cargo).body(), "$.cargo.estado"));
    assertEquals(0, jdbc.queryForObject("select count(*) from pagos", Integer.class));
    jdbc.execute("drop trigger fallo on pagos");
    jdbc.execute("drop function fallar_pago()");
    try (var executor = Executors.newFixedThreadPool(11)) {
      var start = new CountDownLatch(1);
      var futures = new ArrayList<Future<HttpResponse<String>>>();
      for (int i = 0; i < 10; i++)
        futures.add(
            executor.submit(
                () -> {
                  start.await();
                  return post(c, "/pagos", pagoBody(cargo, sesion, "1", UUID.randomUUID()));
                }));
      var cierre =
          executor.submit(
              () -> {
                start.await();
                return post(
                    c,
                    "/sesiones/" + sesion + "/cerrar",
                    "{\"efectivoDeclarado\":100,\"motivoDiferencia\":\"Conteo\"}");
              });
      start.countDown();
      var result = cierre.get(30, TimeUnit.SECONDS);
      assertEquals(200, result.statusCode(), result.body());
      int cobrados = 0;
      for (var f : futures) {
        var res = f.get(30, TimeUnit.SECONDS);
        if (res.statusCode() == 200) cobrados++;
        else assertEquals(409, res.statusCode(), res.body());
      }
      assertEquals(100 + cobrados, numero(result.body(), "$.efectivoEsperado"));
      assertEquals(
          409, post(c, "/pagos", pagoBody(cargo, sesion, "1", UUID.randomUUID())).statusCode());
    }
  }

  @Test
  void cancelacionSinCargoYConCargoNoAnulaDinero() throws Exception {
    var c = login(caj);
    var r = login(rec);
    String cita = cita();
    String cargo = cargo(c, cita);
    var cancel =
        enviar(
            r,
            "PUT",
            "/api/citas/" + cita + "/cancelacion",
            "{\"version\":0,\"motivo\":\"Solicitud\"}",
            true);
    assertEquals(200, cancel.statusCode(), cancel.body());
    assertEquals(true, read(cancel.body(), "$.gestionEconomicaPendiente"));
    assertEquals(40, numero(saldo(c, cargo).body(), "$.saldoPendiente"));
    assertEquals(cargo, cargo(c, cita));
  }

  @Test
  void modalidadSinCargoYReporteSoloCajaPropia() throws Exception {
    jdbc.update("update servicio_modalidades set tarifa=0,con_cargo=false where id=?", modalidad);
    var c = login(caj);
    String cita = cita();
    assertEquals(409, post(c, "/cargos", "{\"citaId\":\"" + cita + "\"}").statusCode());
    assertEquals(0, jdbc.queryForObject("select count(*) from cargos", Integer.class));
    jdbc.update("update citas set importe=40 where id=?", UUID.fromString(cita));
    String cargo = cargo(c, cita);
    String sesion = abrir(c);
    id(post(c, "/pagos", pagoBody(cargo, sesion, "20", UUID.randomUUID())));
    var otro = login(cuenta("CAJ", null));
    String hoy = LocalDate.now(ZoneId.of("America/Lima")).toString();
    String path = "/api/billing/reportes/economico?fechaInicio=" + hoy + "&fechaFin=" + hoy;
    assertEquals(20, numero(enviar(c, "GET", path, null, false).body(), "$.ingresosNetos"));
    assertEquals(0, numero(enviar(otro, "GET", path, null, false).body(), "$.ingresosNetos"));
    assertEquals(
        20, numero(enviar(login(adm), "GET", path, null, false).body(), "$.ingresosNetos"));
  }

  @Test
  void rechazaCitaCanceladaDineroInvalidoYClavesReutilizadas() throws Exception {
    var c = login(caj);
    var r = login(rec);
    String id = cita();
    var cancel =
        enviar(
            r,
            "PUT",
            "/api/citas/" + id + "/cancelacion",
            "{\"version\":0,\"motivo\":\"Solicitud\"}",
            true);
    assertEquals(200, cancel.statusCode(), cancel.body());
    assertEquals(false, read(cancel.body(), "$.gestionEconomicaPendiente"));
    assertEquals(409, post(c, "/cargos", "{\"citaId\":\"" + id + "\"}").statusCode());
    assertEquals(
        400,
        post(c, "/sesiones", "{\"fondoInicial\":1.001,\"clave\":\"" + UUID.randomUUID() + "\"}")
            .statusCode());
    var clave = UUID.randomUUID();
    var body = "{\"fondoInicial\":100,\"cajeroId\":\"" + adm + "\",\"clave\":\"" + clave + "\"}";
    var abierta = post(c, "/sesiones", body);
    assertEquals(200, abierta.statusCode(), abierta.body());
    assertEquals(caj.toString(), read(abierta.body(), "$.cajeroId"));
    assertEquals(id(abierta), id(post(c, "/sesiones", body)));
    assertEquals(409, post(c, "/sesiones", body.replace(":100", ":200")).statusCode());
  }

  @Test
  void reversionFallidaNoQuedaAprobadaNiModificaSaldo() throws Exception {
    var c = login(caj);
    var a = login(adm);
    String cargo = cargo(c, cita());
    String sesion = abrir(c);
    String pago = id(post(c, "/pagos", pagoBody(cargo, sesion, "40", UUID.randomUUID())));
    String solicitud = solicitar(c, pago, "PAGO");
    jdbc.execute(
        "create function fallar_pago() returns trigger language plpgsql as $$ begin raise exception 'fallo sintético'; end $$");
    jdbc.execute(
        "create trigger fallo before insert on pagos for each row execute function fallar_pago()");
    assertEquals(
        500, post(a, "/reversiones/" + solicitud + "/resolver", "{\"aprobar\":true}").statusCode());
    assertEquals(
        "PENDIENTE",
        jdbc.queryForObject(
            "select estado from solicitudes_reversion where id=?",
            String.class,
            UUID.fromString(solicitud)));
    assertEquals(0, numero(saldo(c, cargo).body(), "$.saldoPendiente"));
    assertEquals(0, jdbc.queryForObject("select count(*) from compensaciones_caja", Integer.class));
  }
}
