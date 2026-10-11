package pe.centrosalud.integration;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.time.LocalDate;
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
import pe.centrosalud.modules.administration.application.ports.Pacientes;
import pe.centrosalud.modules.administration.application.usecases.ActualizarPaciente;
import pe.centrosalud.modules.administration.application.usecases.ErrorPacientes;
import pe.centrosalud.modules.administration.application.usecases.RegistrarPaciente;
import pe.centrosalud.modules.administration.domain.Paciente;
import pe.centrosalud.modules.administration.domain.TipoDocumento;
import pe.centrosalud.modules.identity.application.ports.HashContrasenas;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.servlet.session.cookie.secure=false")
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class PacientesHttpTest {
  static final String SCHEMA = "test_pacientes_" + UUID.randomUUID().toString().replace("-", "");

  @DynamicPropertySource
  static void schema(DynamicPropertyRegistry registry) {
    registry.add("spring.flyway.default-schema", () -> SCHEMA);
    registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
  }

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  @Autowired HashContrasenas hashes;
  @Autowired RegistrarPaciente registrar;
  @Autowired ActualizarPaciente actualizarCasoUso;
  @Autowired Pacientes pacientes;
  String password, csrf;
  HttpClient client;

  @BeforeEach
  void preparar() {
    password = UUID.randomUUID().toString();
  }

  @AfterEach
  void limpiar() {
    jdbc.execute(
        "TRUNCATE auditoria_pacientes,pacientes,spring_session,intentos_login,cuentas CASCADE");
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

  static String json(String tipo, String numero, String nombres, String nacimiento) {
    return "{\"tipoDocumento\":\""
        + tipo
        + "\",\"numeroDocumento\":\""
        + numero
        + "\",\"nombres\":\""
        + nombres
        + "\",\"apellidos\":\"Quispe Rojas\",\"fechaNacimiento\":\""
        + nacimiento
        + "\",\"telefono\":\"987654321\",\"correo\":null}";
  }

  HttpResponse<String> alta(String tipo, String numero, String nombres) throws Exception {
    return send("POST", "/api/pacientes", json(tipo, numero, nombres, "1990-05-20"), true);
  }

  int pacientes() {
    return jdbc.queryForObject("SELECT count(*) FROM pacientes", Integer.class);
  }

  @Test
  void recepcionRegistraConCodigoYAuditoriaSinDatosPersonales() throws Exception {
    UUID rec = ingresar("REC");
    var respuesta = alta("DNI", "12345678", "Ana María");
    assertEquals(201, respuesta.statusCode(), respuesta.body());
    String codigo = read(respuesta.body(), "$.codigo");
    assertTrue(codigo.matches("P\\d{8}"), codigo);
    assertEquals("Ana María", read(respuesta.body(), "$.nombres"));
    UUID id = UUID.fromString(read(respuesta.body(), "$.id"));
    var auditoria = jdbc.queryForMap("SELECT * FROM auditoria_pacientes WHERE paciente_id=?", id);
    assertEquals(rec, auditoria.get("actor_id"));
    assertEquals("CREACION", auditoria.get("tipo"));
    assertEquals(Set.of("id", "actor_id", "paciente_id", "tipo", "fecha"), auditoria.keySet());
    assertEquals(
        rec, jdbc.queryForObject("SELECT creado_por FROM pacientes WHERE id=?", UUID.class, id));
  }

  @Test
  void documentosDistintosPermitenHomonimosYDuplicadoSeRechaza() throws Exception {
    ingresar("REC");
    assertEquals(201, alta("DNI", "12345678", "Ana").statusCode());
    assertEquals(201, alta("DNI", "87654321", "Ana").statusCode());
    assertEquals(201, alta("CE", "123456789", "Ana").statusCode());
    var duplicado = alta("DNI", "12345678", "Otra Persona");
    assertEquals(409, duplicado.statusCode());
    assertEquals("DOCUMENTO_DUPLICADO", read(duplicado.body(), "$.code"));
    assertFalse(duplicado.body().contains("12345678"));
    assertEquals(201, alta("CE", "AB1234567", "Luis").statusCode());
    assertEquals(409, alta("CE", "ab1234567", "Luis").statusCode());
    assertEquals(4, pacientes());
    assertEquals(4, jdbc.queryForObject("SELECT count(*) FROM auditoria_pacientes", Integer.class));
  }

  @Test
  void diezAltasConcurrentesDelMismoDocumentoCreanUnSoloPaciente() throws Exception {
    UUID rec = cuenta("REC");
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(10)) {
      List<Future<String>> tareas = new ArrayList<>();
      for (int i = 0; i < 10; i++)
        tareas.add(
            pool.submit(
                () -> {
                  inicio.await();
                  try {
                    registrar.registrar(
                        rec,
                        TipoDocumento.DNI,
                        "11112222",
                        "Ana",
                        "Quispe",
                        LocalDate.of(1990, 1, 1),
                        null,
                        null);
                    return "OK";
                  } catch (ErrorPacientes e) {
                    return e.codigo().name();
                  }
                }));
      inicio.countDown();
      List<String> resultados = new ArrayList<>();
      for (var tarea : tareas) resultados.add(tarea.get(15, TimeUnit.SECONDS));
      assertEquals(1, Collections.frequency(resultados, "OK"));
      assertEquals(9, Collections.frequency(resultados, "DOCUMENTO_DUPLICADO"));
    }
    assertEquals(1, pacientes());
    assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM auditoria_pacientes", Integer.class));
  }

  @Test
  void altasConcurrentesDeDocumentosDistintosRecibenCodigosDistintos() throws Exception {
    UUID rec = cuenta("REC");
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(10)) {
      List<Future<String>> tareas = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        String numero = "2000000" + i;
        tareas.add(
            pool.submit(
                () -> {
                  inicio.await();
                  return registrar
                      .registrar(
                          rec,
                          TipoDocumento.DNI,
                          numero,
                          "Ana",
                          "Quispe",
                          LocalDate.of(1990, 1, 1),
                          null,
                          null)
                      .codigo();
                }));
      }
      inicio.countDown();
      Set<String> codigos = new HashSet<>();
      for (var tarea : tareas) codigos.add(tarea.get(15, TimeUnit.SECONDS));
      assertEquals(10, codigos.size());
    }
  }

  @Test
  void falloDeAuditoriaRevierteElAlta() throws Exception {
    ingresar("REC");
    jdbc.execute(
        "ALTER TABLE auditoria_pacientes ADD CONSTRAINT simular_fallo CHECK(false) NOT VALID");
    try {
      assertEquals(503, alta("DNI", "12345678", "Ana").statusCode());
      assertEquals(0, pacientes());
    } finally {
      jdbc.execute("ALTER TABLE auditoria_pacientes DROP CONSTRAINT simular_fallo");
    }
    assertEquals(201, alta("DNI", "12345678", "Ana").statusCode());
  }

  @Test
  void soloRecepcionPuedeRegistrarAunqueTengaOtrosRoles() throws Exception {
    for (String rol : List.of("ADM", "CAJ", "PRO")) {
      ingresar(rol);
      assertEquals(403, alta("DNI", "12345678", "Ana").statusCode(), rol);
    }
    ingresar();
    assertEquals(403, alta("DNI", "12345678", "Ana").statusCode());
    assertEquals(0, pacientes());
    ingresar("ADM", "REC");
    assertEquals(201, alta("DNI", "12345678", "Ana").statusCode());
  }

  @Test
  void exigeSesionCsrfYPermisosVigentes() throws Exception {
    client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
    token();
    assertEquals(401, alta("DNI", "12345678", "Ana").statusCode());
    assertEquals(401, send("GET", "/api/pacientes", null, false).statusCode());
    UUID rec = ingresar("REC");
    assertEquals(
        403,
        send("POST", "/api/pacientes", json("DNI", "12345678", "Ana", "1990-05-20"), false)
            .statusCode());
    // La búsqueda (RF-05) no exige CSRF, pero sí el permiso: con REC ya autenticado, se permite.
    assertEquals(200, send("GET", "/api/pacientes", null, false).statusCode());
    jdbc.update("DELETE FROM cuenta_roles WHERE cuenta_id=?", rec);
    assertEquals(403, alta("DNI", "12345678", "Ana").statusCode());
    assertEquals(403, send("GET", "/api/pacientes", null, false).statusCode());
    UUID otra = ingresar("REC");
    jdbc.update("UPDATE cuentas SET activa=false WHERE id=?", otra);
    assertEquals(401, alta("DNI", "12345678", "Ana").statusCode());
    assertEquals(401, send("GET", "/api/pacientes", null, false).statusCode());
    assertEquals(0, pacientes());
  }

  @Test
  void rechazaDatosInvalidos() throws Exception {
    ingresar("REC");
    String futuro = LocalDate.now().plusDays(2).toString();
    for (String body :
        List.of(
            json("DNI", "1234567", "Ana", "1990-05-20"),
            json("DNI", "12345678", "Ana", futuro),
            json("DNI", "12345678", "", "1990-05-20"),
            json("PASAPORTE", "12345678", "Ana", "1990-05-20"),
            json("DNI", "12345678", "Ana", "20-05-1990"),
            "{\"tipoDocumento\":")) {
      assertEquals(400, send("POST", "/api/pacientes", body, true).statusCode(), body);
    }
    assertEquals(0, pacientes());
  }

  HttpResponse<String> editarHttp(UUID id, String tipo, String numero, String nombres, long version)
      throws Exception {
    return send(
        "PUT",
        "/api/pacientes/" + id,
        "{\"tipoDocumento\":\""
            + tipo
            + "\",\"numeroDocumento\":\""
            + numero
            + "\",\"nombres\":\""
            + nombres
            + "\",\"apellidos\":\"Quispe Rojas\",\"fechaNacimiento\":\"1990-05-20\",\"telefono\":null,\"correo\":null,\"version\":"
            + version
            + "}",
        true);
  }

  @Test
  void buscaPorCodigoDocumentoONombreConPaginacion() throws Exception {
    ingresar("REC");
    var pacienteA = alta("DNI", "10000001", "Ana María Soto");
    alta("DNI", "10000002", "Luis Soto");
    String codigoA = read(pacienteA.body(), "$.codigo");
    assertEquals(201, pacienteA.statusCode());
    var porDocumento = send("GET", "/api/pacientes?busqueda=10000001", null, false);
    assertEquals(1, ((List<?>) read(porDocumento.body(), "$.elementos")).size());
    var porNombre = send("GET", "/api/pacientes?busqueda=Soto", null, false);
    assertEquals(2, ((List<?>) read(porNombre.body(), "$.elementos")).size());
    var porCodigo = send("GET", "/api/pacientes?busqueda=" + codigoA, null, false);
    assertEquals(1, ((List<?>) read(porCodigo.body(), "$.elementos")).size());
    assertFalse((Boolean) read(porCodigo.body(), "$.hayMas"));
  }

  @Test
  void actualizarConservaCodigoYVuelveAVerificarUnicidad() throws Exception {
    ingresar("REC");
    var a = alta("DNI", "20000001", "Ana");
    var b = alta("DNI", "20000002", "Luis");
    String codigoOriginal = read(a.body(), "$.codigo");
    UUID idA = UUID.fromString(read(a.body(), "$.id"));
    assertEquals(201, b.statusCode());
    var editado = editarHttp(idA, "DNI", "20000001", "Ana María", 0);
    assertEquals(200, editado.statusCode(), editado.body());
    assertEquals(codigoOriginal, read(editado.body(), "$.codigo"), "El código interno no cambia");
    assertEquals(1, (Integer) read(editado.body(), "$.version"));
    // Cambiar el documento al de otro paciente existente vuelve a verificar unicidad (CA-2).
    var duplicado = editarHttp(idA, "DNI", "20000002", "Ana María", 1);
    assertEquals(409, duplicado.statusCode());
    assertEquals("DOCUMENTO_DUPLICADO", read(duplicado.body(), "$.code"));
    assertFalse(duplicado.body().contains("20000002"));
    // Un cambio de documento a uno nuevo se acepta y mantiene el código.
    var nuevoDocumento = editarHttp(idA, "CE", "ab1234567", "Ana María", 1);
    assertEquals(200, nuevoDocumento.statusCode(), nuevoDocumento.body());
    assertEquals(codigoOriginal, read(nuevoDocumento.body(), "$.codigo"));
  }

  @Test
  void versionObsoletaRecibeConflictoYGuardadoIdenticoNoAudita() throws Exception {
    ingresar("REC");
    // alta() fija el teléfono en "987654321"; repetir esos mismos datos es un guardado idéntico.
    var alta = alta("DNI", "30000001", "Ana");
    UUID id = UUID.fromString(read(alta.body(), "$.id"));
    var identico =
        send(
            "PUT",
            "/api/pacientes/" + id,
            "{\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"30000001\",\"nombres\":\"Ana\",\"apellidos\":\"Quispe Rojas\","
                + "\"fechaNacimiento\":\"1990-05-20\",\"telefono\":\"987654321\",\"correo\":null,\"version\":0}",
            true);
    assertEquals(200, identico.statusCode(), identico.body());
    assertEquals(
        0,
        (Integer) read(identico.body(), "$.version"),
        "Un guardado idéntico no incrementa la versión");
    assertEquals(200, editarHttp(id, "DNI", "30000001", "Ana", 0).statusCode());
    var obsoleta = editarHttp(id, "DNI", "30000001", "Otro Nombre", 0);
    assertEquals(409, obsoleta.statusCode());
    assertEquals("CONFLICTO", read(obsoleta.body(), "$.code"));
    assertEquals(1L, pacientes.buscarPorId(id).orElseThrow().version());
    assertEquals(200, editarHttp(id, "DNI", "30000001", "Ana María", 1).statusCode());
    assertEquals(2L, pacientes.buscarPorId(id).orElseThrow().version());
    assertEquals(
        3,
        jdbc.queryForObject("SELECT count(*) FROM auditoria_pacientes", Integer.class),
        "Alta + dos ediciones reales; el guardado idéntico y el conflicto no auditan");
  }

  @Test
  void diezEdicionesConcurrentesConLaMismaVersionSoloAplicanUna() throws Exception {
    UUID rec = cuenta("REC");
    var paciente =
        registrar.registrar(
            rec,
            TipoDocumento.DNI,
            "40000001",
            "Ana",
            "Quispe",
            LocalDate.of(1990, 1, 1),
            null,
            null);
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
                    actualizarCasoUso.actualizar(
                        rec,
                        paciente.id(),
                        TipoDocumento.DNI,
                        "40000001",
                        nombre,
                        "Quispe",
                        LocalDate.of(1990, 1, 1),
                        null,
                        null,
                        0);
                    return "OK";
                  } catch (ErrorPacientes e) {
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
    assertEquals(1, pacientes.buscarPorId(paciente.id()).orElseThrow().version());
  }

  @Test
  void diezEdicionesConcurrentesQueCambianElDocumentoAlMismoValorSoloAplicanUna() throws Exception {
    UUID rec = cuenta("REC");
    var destino = "50000099";
    List<Paciente> pacientesCreados = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      pacientesCreados.add(
          registrar.registrar(
              rec,
              TipoDocumento.DNI,
              "5000000" + i,
              "Ana" + i,
              "Quispe",
              LocalDate.of(1990, 1, 1),
              null,
              null));
    }
    var inicio = new CountDownLatch(1);
    try (var pool = Executors.newFixedThreadPool(10)) {
      List<Future<String>> tareas = new ArrayList<>();
      for (Paciente paciente : pacientesCreados) {
        tareas.add(
            pool.submit(
                () -> {
                  inicio.await();
                  try {
                    actualizarCasoUso.actualizar(
                        rec,
                        paciente.id(),
                        TipoDocumento.DNI,
                        destino,
                        paciente.nombres(),
                        paciente.apellidos(),
                        paciente.fechaNacimiento(),
                        null,
                        null,
                        0);
                    return "OK";
                  } catch (ErrorPacientes e) {
                    return e.codigo().name();
                  }
                }));
      }
      inicio.countDown();
      List<String> resultados = new ArrayList<>();
      for (var tarea : tareas) resultados.add(tarea.get(15, TimeUnit.SECONDS));
      assertEquals(1, Collections.frequency(resultados, "OK"), resultados.toString());
      assertEquals(
          9, Collections.frequency(resultados, "DOCUMENTO_DUPLICADO"), resultados.toString());
    }
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT count(*) FROM pacientes WHERE numero_documento=?", Integer.class, destino));
  }

  @Test
  void soloRecepcionBuscaYActualizaPacientes() throws Exception {
    UUID rec = cuenta("REC");
    var paciente =
        registrar.registrar(
            rec,
            TipoDocumento.DNI,
            "60000001",
            "Ana",
            "Quispe",
            LocalDate.of(1990, 1, 1),
            null,
            null);
    String editar = "/api/pacientes/" + paciente.id();
    for (String rol : List.of("ADM", "CAJ", "PRO", "")) {
      if (rol.isEmpty()) ingresar();
      else ingresar(rol);
      assertEquals(403, send("GET", "/api/pacientes", null, false).statusCode(), rol);
      assertEquals(403, editarHttp(paciente.id(), "DNI", "60000001", "Ana", 0).statusCode(), rol);
    }
    assertEquals(0L, pacientes.buscarPorId(paciente.id()).orElseThrow().version());
    ingresar("ADM", "REC");
    assertEquals(200, send("GET", "/api/pacientes", null, false).statusCode());
    assertEquals(200, editarHttp(paciente.id(), "DNI", "60000001", "Ana María", 0).statusCode());
  }
}
