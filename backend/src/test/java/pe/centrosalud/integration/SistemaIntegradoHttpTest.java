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
@Import(SistemaIntegradoHttpTest.Tiempo.class)
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class SistemaIntegradoHttpTest {
  static final String SCHEMA =
      "test_sistema_integrado_" + UUID.randomUUID().toString().replace("-", "");

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
  String password;
  UUID admin;
  Sesion adm, rec, pro, caj;
  String recepcionId, profesionalCuentaId, cajeroId;
  String pacienteId, servicioId, profesionalId, consultorioId, modalidadId, gratuitaId;

  record Sesion(HttpClient client, String csrf) {}

  @BeforeEach
  void preparar() throws Exception {
    password = UUID.randomUUID().toString();
    admin = UUID.randomUUID();
    // Único dato de arranque: una cuenta ADM. El resto del escenario se crea por HTTP.
    jdbc.update(
        "INSERT INTO cuentas(id,identificador,activa,contrasena_hash) VALUES (?,?,true,?)",
        admin,
        admin.toString(),
        hashes.generar(password));
    jdbc.update("INSERT INTO cuenta_roles VALUES (?,'ADM')", admin);
    adm = login(admin);
    recepcionId = crearCuenta("REC");
    profesionalCuentaId = crearCuenta("PRO");
    cajeroId = crearCuenta("CAJ");
    rec = login(UUID.fromString(recepcionId));
    pro = login(UUID.fromString(profesionalCuentaId));
    caj = login(UUID.fromString(cajeroId));
    pacienteId = id(enviar(rec, "POST", "/api/pacientes", paciente("Paciente"), true), 201);
    var servicio = enviar(adm, "POST", "/api/servicios", servicio(40, 30, 0), true);
    servicioId = id(servicio, 201);
    modalidadId =
        ((java.util.List<String>)
                read(servicio.body(), "$.modalidades[?(@.nombre == 'Particular')].id"))
            .getFirst();
    gratuitaId =
        ((java.util.List<String>)
                read(servicio.body(), "$.modalidades[?(@.nombre == 'Gratuita')].id"))
            .getFirst();
    profesionalId =
        id(
            enviar(
                adm,
                "POST",
                "/api/profesionales/fichas",
                """
      {"identificacion":"PRO-SINTETICO","nombre":"Profesional Sintético","funcion":"Medicina","serviciosHabilitados":["%s"]}
      """
                    .formatted(servicioId),
                true),
            201);
    consultorioId =
        id(
            enviar(
                adm,
                "POST",
                "/api/consultorios",
                """
      {"codigo":"CS1","nombre":"Consultorio Sintético","serviciosCompatibles":["%s"]}
      """
                    .formatted(servicioId),
                true),
            201);
    comprobar(
        enviar(
            adm,
            "PUT",
            "/api/cuentas/" + profesionalCuentaId + "/profesional",
            """
      {"profesionalId":"%s","version":%d}
      """
                .formatted(profesionalId, versionCuenta(profesionalCuentaId)),
            true),
        200);
    for (var entrada : List.of("profesionales/" + profesionalId, "consultorios/" + consultorioId))
      comprobar(
          enviar(
              adm,
              "POST",
              "/api/disponibilidad/" + entrada,
              """
      {"fecha":"2026-10-12","horaInicio":"08:00","horaFin":"12:00","tipo":"DISPONIBLE"}
      """,
              true),
          201);
  }

  @AfterEach
  void limpiar() {
    jdbc.execute(
        "TRUNCATE cargos,sesiones_caja,solicitudes_reversion,reintentos_caja,compensaciones_caja,citas,reprogramaciones_cita,cuentas,pacientes,profesionales,servicios,consultorios,spring_session,intentos_login CASCADE");
  }

  @AfterAll
  static void cerrar(@Autowired JdbcTemplate jdbc) {
    jdbc.execute("DROP SCHEMA " + SCHEMA + " CASCADE");
  }

  String crearCuenta(String rol) throws Exception {
    String nombre = UUID.randomUUID().toString();
    // Identificadores sintéticos para login de las cuentas creadas mediante la API.
    var r =
        enviar(
            adm,
            "POST",
            "/api/cuentas",
            """
      {"identificador":"%s","contrasena":"%s","roles":["%s"]}
      """
                .formatted(nombre, password, rol),
            true);
    String id = id(r, 201);
    identificadores.put(id, nombre);
    return id;
  }

  Map<String, String> identificadores = new HashMap<>();

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
                "{\"identificador\":\""
                    + identificadores.getOrDefault(id.toString(), id.toString())
                    + "\",\"contrasena\":\""
                    + password
                    + "\"}",
                true)
            .statusCode());
    return new Sesion(c, read(enviar(s, "GET", "/api/auth/csrf", null, false).body(), "$.token"));
  }

  long versionCuenta(String id) throws Exception {
    var r = enviar(adm, "GET", "/api/cuentas/" + id + "/roles", null, false);
    comprobar(r, 200);
    return ((Number) read(r.body(), "$.version")).longValue();
  }

  void comprobar(HttpResponse<String> r, int status) {
    assertEquals(status, r.statusCode(), r.body());
  }

  String id(HttpResponse<String> r, int status) {
    comprobar(r, status);
    return read(r.body(), "$.id");
  }

  double numero(HttpResponse<String> r, String path) {
    comprobar(r, 200);
    return ((Number) read(r.body(), path)).doubleValue();
  }

  String paciente(String nombre) {
    return """
      {"tipoDocumento":"DNI","numeroDocumento":"12345678","nombres":"%s","apellidos":"Sintético","fechaNacimiento":"2000-01-01","telefono":"999999999","correo":"sintetico@example.invalid"}
      """
        .formatted(nombre);
  }

  String servicio(int tarifa, int duracion, long version) {
    return """
      {"nombre":"Consulta Sintética","duracionMinutos":%d,"activo":true,"version":%d,"modalidades":[
       {"id":%s,"nombre":"Particular","conCargo":true,"tarifa":%d,"activa":true},
       {"id":%s,"nombre":"Gratuita","conCargo":false,"tarifa":0,"activa":true}]}
      """
        .formatted(
            duracion,
            version,
            modalidadId == null ? "null" : "\"" + modalidadId + "\"",
            tarifa,
            gratuitaId == null ? "null" : "\"" + gratuitaId + "\"");
  }

  String reservar(String hora, String modalidad) throws Exception {
    return id(
        enviar(
            rec,
            "POST",
            "/api/citas",
            """
      {"pacienteId":"%s","profesionalId":"%s","servicioId":"%s","consultorioId":"%s","modalidad":"%s","fecha":"2026-10-12","hora":"%s"}
      """
                .formatted(pacienteId, profesionalId, servicioId, consultorioId, modalidad, hora),
            true),
        201);
  }

  HttpResponse<String> agenda(Sesion s) throws Exception {
    return enviar(s, "GET", "/api/citas/agenda?fecha=2026-10-12", null, false);
  }

  void atender(String cita, long version) throws Exception {
    comprobar(
        enviar(
            rec, "PUT", "/api/citas/" + cita + "/llegada", "{\"version\":" + version + "}", true),
        200);
    comprobar(
        enviar(
            pro,
            "PUT",
            "/api/citas/" + cita + "/atencion/inicio",
            "{\"version\":" + (version + 1) + "}",
            true),
        200);
    comprobar(
        enviar(
            pro,
            "PUT",
            "/api/citas/" + cita + "/atencion/finalizacion",
            "{\"version\":" + (version + 2) + "}",
            true),
        200);
  }

  String cargo(String cita) throws Exception {
    return id(
        enviar(caj, "POST", "/api/billing/cargos", "{\"citaId\":\"" + cita + "\"}", true), 200);
  }

  String abrir() throws Exception {
    return id(
        enviar(
            caj,
            "POST",
            "/api/billing/sesiones",
            "{\"fondoInicial\":100,\"clave\":\"" + UUID.randomUUID() + "\"}",
            true),
        200);
  }

  String pagar(String cargo, String sesion, int importe) throws Exception {
    return id(
        enviar(
            caj,
            "POST",
            "/api/billing/pagos",
            """
      {"cargoId":"%s","sesionCajaId":"%s","importe":%d,"medio":"EFECTIVO","clave":"%s"}
      """
                .formatted(cargo, sesion, importe, UUID.randomUUID()),
            true),
        200);
  }

  HttpResponse<String> saldo(String cargo) throws Exception {
    return enviar(caj, "GET", "/api/billing/cargos/" + cargo + "/saldo", null, false);
  }

  HttpResponse<String> cerrar(String sesion, int efectivo) throws Exception {
    var r =
        enviar(
            caj,
            "POST",
            "/api/billing/sesiones/" + sesion + "/cerrar",
            "{\"efectivoDeclarado\":" + efectivo + "}",
            true);
    comprobar(r, 200);
    return r;
  }

  HttpResponse<String> dashboard(Sesion s) throws Exception {
    return enviar(s, "GET", "/api/citas/dashboard?fecha=2026-10-12", null, false);
  }

  @Test
  void desdeAltaDeRecursosHastaAtencionCobroDevolucionYCierres() throws Exception {
    // Paciente real: registro, búsqueda y rechazo de duplicado.
    comprobar(enviar(rec, "POST", "/api/pacientes", paciente("Paciente"), true), 409);
    var buscar = enviar(rec, "GET", "/api/pacientes?busqueda=Sint%C3%A9tico", null, false);
    comprobar(buscar, 200);
    assertEquals(pacienteId, read(buscar.body(), "$.elementos[0].id"));
    String cita = reservar("09:00", modalidadId);
    assertEquals(40, numero(agenda(rec), "$[0].importe"));
    // Reprogramación preserva tarifa y duración; el catálogo ya tiene otros valores.
    comprobar(enviar(adm, "PUT", "/api/servicios/" + servicioId, servicio(70, 45, 0), true), 200);
    comprobar(
        enviar(
            rec,
            "POST",
            "/api/citas/" + cita + "/reprogramaciones",
            "{\"fecha\":\"2026-10-12\",\"hora\":\"10:00\",\"motivo\":\"Paciente solicita\",\"version\":0}",
            true),
        200);
    assertEquals(40, numero(agenda(rec), "$[0].importe"));
    assertEquals(30, numero(agenda(rec), "$[0].duracionMinutos"));
    String cargo = cargo(cita);
    String sesion = abrir();
    String pago = pagar(cargo, sesion, 15);
    assertEquals(25, numero(saldo(cargo), "$.saldoPendiente"));
    // La deuda parcial no impide la atención del profesional vinculado.
    atender(cita, 1);
    assertEquals("ATENDIDA", read(agenda(pro).body(), "$[0].estado"));
    assertEquals(1, numero(dashboard(rec), "$.atendidas"));
    pagar(cargo, sesion, 25);
    assertEquals(0, numero(saldo(cargo), "$.saldoPendiente"));
    assertEquals(140, numero(cerrar(sesion, 140), "$.efectivoEsperado"));
    String solicitud =
        id(
            enviar(
                caj,
                "POST",
                "/api/billing/reversiones",
                "{\"operacionId\":\""
                    + pago
                    + "\",\"tipo\":\"PAGO\",\"motivo\":\"Devolución de pago registrado\"}",
                true),
            200);
    comprobar(
        enviar(
            adm,
            "POST",
            "/api/billing/reversiones/" + solicitud + "/resolver",
            "{\"aprobar\":true}",
            true),
        409);
    String segunda = abrir();
    comprobar(
        enviar(
            adm,
            "POST",
            "/api/billing/reversiones/" + solicitud + "/resolver",
            "{\"aprobar\":true}",
            true),
        200);
    assertEquals(15, numero(saldo(cargo), "$.saldoPendiente"));
    assertEquals(
        140,
        numero(
            enviar(adm, "GET", "/api/billing/sesiones/" + sesion, null, false),
            "$.efectivoEsperado"));
    assertEquals(85, numero(cerrar(segunda, 85), "$.efectivoEsperado"));
    String hoy = LocalDate.now(ZoneId.of("America/Lima")).toString();
    var reporte =
        enviar(
            adm,
            "GET",
            "/api/billing/reportes/economico?fechaInicio=" + hoy + "&fechaFin=" + hoy,
            null,
            false);
    assertEquals(40, numero(reporte, "$.totalCobros"));
    assertEquals(15, numero(reporte, "$.totalDevoluciones"));
    assertEquals(25, numero(reporte, "$.ingresosNetos"));
    // Corregir el nombre conserva la relación; caja y agenda reflejan el nombre actual.
    var datos = paciente("Nombre Corregido").strip();
    datos = datos.substring(0, datos.length() - 1) + ",\"version\":0}";
    comprobar(enviar(rec, "PUT", "/api/pacientes/" + pacienteId, datos, true), 200);
    assertEquals("Nombre Corregido Sintético", read(agenda(pro).body(), "$[0].paciente"));
    var contexto = enviar(caj, "GET", "/api/billing/contexto?fecha=2026-10-12", null, false);
    comprobar(contexto, 200);
    assertEquals("Nombre Corregido Sintético", read(contexto.body(), "$.citas[0].cita.paciente"));
    assertEquals(cargo, read(contexto.body(), "$.citas[0].cargos[0].cargo.id"));
  }

  @Test
  void gratuitaAvanzaSinCajaYPermisosSeAplicanEntreModulos() throws Exception {
    String cita = reservar("09:00", gratuitaId);
    atender(cita, 0);
    assertEquals(1, numero(dashboard(pro), "$.atendidas"));
    comprobar(
        enviar(caj, "POST", "/api/billing/cargos", "{\"citaId\":\"" + cita + "\"}", true), 409);
    var contexto = enviar(caj, "GET", "/api/billing/contexto?fecha=2026-10-12", null, false);
    comprobar(contexto, 200);
    assertTrue(((List<?>) read(contexto.body(), "$.citas[0].cargos")).isEmpty());
    comprobar(enviar(caj, "GET", "/api/pacientes?busqueda=Sintetico", null, false), 403);
    comprobar(enviar(rec, "GET", "/api/billing/contexto?fecha=2026-10-12", null, false), 403);
    comprobar(enviar(pro, "GET", "/api/billing/contexto?fecha=2026-10-12", null, false), 403);
    var ambito =
        enviar(
            pro,
            "GET",
            "/api/citas/agenda?fecha=2026-10-12&profesionalId=" + UUID.randomUUID(),
            null,
            false);
    comprobar(ambito, 200);
    assertEquals(List.of(profesionalId), read(ambito.body(), "$[*].profesionalId"));
    // Revocar CAJ por la API de roles retira el acceso en la misma sesión.
    comprobar(
        enviar(
            adm,
            "PUT",
            "/api/cuentas/" + cajeroId + "/roles",
            "{\"roles\":[\"REC\"],\"version\":" + versionCuenta(cajeroId) + "}",
            true),
        200);
    comprobar(enviar(caj, "GET", "/api/billing/contexto?fecha=2026-10-12", null, false), 403);
    // Desactivar una cuenta PRO invalida su sesión para todos los módulos.
    String identificador = identificadores.get(profesionalCuentaId);
    comprobar(
        enviar(
            adm,
            "PUT",
            "/api/cuentas/" + profesionalCuentaId,
            "{\"identificador\":\""
                + identificador
                + "\",\"activa\":false,\"version\":"
                + versionCuenta(profesionalCuentaId)
                + "}",
            true),
        200);
    comprobar(agenda(pro), 401);
  }
}
