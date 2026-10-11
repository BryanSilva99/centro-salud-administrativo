package pe.centrosalud.integration;

import static com.jayway.jsonpath.JsonPath.read;
import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.centrosalud.adapters.outbound.postgres.identity.ControlIntentosPostgres;
import pe.centrosalud.modules.identity.application.ports.*;
import pe.centrosalud.modules.identity.application.usecases.*;
import pe.centrosalud.modules.identity.domain.Cuenta;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.servlet.session.cookie.secure=false")
@Import(SesionHttpTest.Tiempo.class)
@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class SesionHttpTest {
  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;
  @Autowired HashContrasenas hashes;
  @Autowired ControlIntentos intentos;
  @Autowired Reloj reloj;
  private HttpClient client;
  private CookieManager cookies;
  private UUID id;
  private String nombre, clave;
  private String csrf;
  private final Set<String> sesiones = new HashSet<>();
  private final Set<String> identificadores = new HashSet<>();

  static class Reloj extends Clock {
    private volatile Instant ahora = Instant.now();

    void avanzar(long seconds) {
      ahora = ahora.plusSeconds(seconds);
    }

    void reset() {
      ahora = Instant.now();
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return ahora;
    }
  }

  @TestConfiguration
  static class Tiempo {
    @Bean
    @Primary
    Reloj relojPrueba() {
      return new Reloj();
    }
  }

  @BeforeEach
  void preparar() throws Exception {
    reloj.reset();
    id = UUID.randomUUID();
    nombre = "http-" + id;
    clave = UUID.randomUUID().toString();
    identificadores.add(nombre);
    jdbc.update(
        "INSERT INTO cuentas(id, identificador, activa, contrasena_hash) VALUES (?, ?, true, ?)",
        id,
        nombre,
        hashes.generar(clave));
    cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    client =
        HttpClient.newBuilder()
            .cookieHandler(cookies)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    renovarCsrf();
  }

  @AfterEach
  void limpiar() {
    for (String identificador : identificadores)
      jdbc.update(
          "DELETE FROM intentos_login WHERE clave = ?",
          ControlIntentosPostgres.clave(identificador));
    jdbc.update("DELETE FROM spring_session WHERE principal_name = ?", id.toString());
    for (String session : sesiones)
      jdbc.update("DELETE FROM spring_session WHERE session_id = ?", session);
    jdbc.update("DELETE FROM cuentas WHERE id = ?", id);
    reloj.reset();
  }

  private HttpResponse<String> enviar(String metodo, String path, String body, boolean token)
      throws Exception {
    var builder =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(15));
    if (token) builder.header("X-CSRF-TOKEN", csrf);
    if (body != null) builder.header("Content-Type", "application/json");
    builder.method(
        metodo,
        body == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body));
    var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    recordarSesiones();
    return response;
  }

  private void recordarSesiones() {
    for (HttpCookie c : cookies.getCookieStore().getCookies()) {
      if (c.getName().equals("SESSION") && !c.getValue().isEmpty()) {
        String valor = c.getValue().replace("\"", "");
        sesiones.add(
            new String(Base64.getDecoder().decode(valor), java.nio.charset.StandardCharsets.UTF_8));
      }
    }
  }

  private void renovarCsrf() throws Exception {
    var response = enviar("GET", "/api/auth/csrf", null, false);
    assertEquals(200, response.statusCode());
    csrf = read(response.body(), "$.token");
  }

  private HttpResponse<String> login(String usuario, String password) throws Exception {
    return enviar(
        "POST",
        "/api/auth/login",
        "{\"identificador\":\"" + usuario + "\",\"contrasena\":\"" + password + "\"}",
        true);
  }

  private String cookie() {
    return cookies.getCookieStore().getCookies().stream()
        .filter(c -> c.getName().equals("SESSION"))
        .findFirst()
        .orElseThrow()
        .getValue();
  }

  @Test
  void loginPersisteRotaSesionYLogoutImpideReutilizarCookie() throws Exception {
    String anterior = cookie();
    var login = login(nombre, clave);
    assertEquals(200, login.statusCode(), login.body());
    assertNotEquals(anterior, cookie());
    assertTrue(
        login.headers().allValues("set-cookie").stream()
            .anyMatch(v -> v.contains("HttpOnly") && v.contains("SameSite=Strict")));
    assertFalse(login.body().contains("hash"));
    assertFalse(login.body().contains(clave));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT count(*) FROM spring_session WHERE principal_name = ?",
            Integer.class,
            id.toString()));
    assertEquals(id.toString(), read(enviar("GET", "/api/auth/me", null, false).body(), "$.id"));
    assertEquals(403, enviar("GET", "/api/cuentas/" + id, null, false).statusCode());
    String autenticada = cookie();
    renovarCsrf();
    assertEquals(204, enviar("POST", "/api/auth/logout", null, true).statusCode());
    assertEquals(401, enviar("GET", "/api/auth/me", null, false).statusCode());
    var replay =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/me"))
            .header("Cookie", "SESSION=" + autenticada)
            .GET()
            .build();
    assertEquals(
        401,
        HttpClient.newHttpClient().send(replay, HttpResponse.BodyHandlers.ofString()).statusCode());
  }

  @Test
  void csrfProtegeLoginLogoutYSeRenuevaAlAutenticar() throws Exception {
    String body = "{\"identificador\":\"" + nombre + "\",\"contrasena\":\"" + clave + "\"}";
    assertEquals(403, enviar("POST", "/api/auth/login", body, false).statusCode());
    String previo = csrf;
    assertEquals(200, login(nombre, clave).statusCode());
    assertEquals(403, enviar("POST", "/api/auth/logout", null, false).statusCode());
    assertEquals(403, enviar("POST", "/api/auth/logout", null, true).statusCode());
    renovarCsrf();
    assertNotEquals(previo, csrf);
    assertEquals(204, enviar("POST", "/api/auth/logout", null, true).statusCode());
  }

  @Test
  void cincoFallosBloqueanInclusoLaClaveCorrectaYExpiran() throws Exception {
    for (int i = 0; i < 5; i++) assertEquals(401, login(nombre, clave + "x").statusCode());
    var bloqueado = login(nombre, clave);
    assertEquals(401, bloqueado.statusCode());
    assertEquals("Identificador o contraseña incorrectos", read(bloqueado.body(), "$.detail"));
    reloj.avanzar(899);
    assertEquals(401, login(nombre, clave).statusCode());
    reloj.avanzar(1);
    assertEquals(200, login(nombre, clave).statusCode());
  }

  @Test
  void exitoReiniciaFallosYAusenciaTieneElMismoError() throws Exception {
    for (int i = 0; i < 4; i++) assertEquals(401, login(nombre, clave + "x").statusCode());
    String ausente = "ausente-" + id;
    identificadores.add(ausente);
    assertEquals(401, login(ausente, clave).statusCode());
    assertEquals(200, login(nombre, clave).statusCode());
    renovarCsrf();
    assertEquals(401, login(nombre, clave + "x").statusCode());
    assertEquals(200, login(nombre, clave).statusCode());
  }

  @Test
  void consultaAutomaticaNoRenuevaLosTreintaMinutos() throws Exception {
    assertEquals(200, login(nombre, clave).statusCode());
    reloj.avanzar(1799);
    assertEquals(200, enviar("GET", "/api/auth/me", null, false).statusCode());
    reloj.avanzar(1);
    assertEquals(401, enviar("GET", "/api/auth/me", null, false).statusCode());
  }

  @Test
  void actividadExplicitaRenuevaInactividadPeroNoLasOchoHoras() throws Exception {
    assertEquals(200, login(nombre, clave).statusCode());
    renovarCsrf();
    for (int i = 0; i < 31; i++) {
      reloj.avanzar(900);
      assertEquals(204, enviar("POST", "/api/auth/activity", null, true).statusCode());
    }
    reloj.avanzar(899);
    assertEquals(200, enviar("GET", "/api/auth/me", null, false).statusCode());
    reloj.avanzar(1);
    assertEquals(401, enviar("GET", "/api/auth/me", null, false).statusCode());
  }

  @Test
  void desactivarYReactivarRevocaLaSesionAnterior() throws Exception {
    assertEquals(200, login(nombre, clave).statusCode());
    jdbc.update("UPDATE cuentas SET activa = false WHERE id = ?", id);
    jdbc.update("UPDATE cuentas SET activa = true WHERE id = ?", id);
    assertEquals(401, enviar("GET", "/api/auth/me", null, false).statusCode());
  }

  @Test
  void cambiarHashRevocaSesionAnterior() throws Exception {
    assertEquals(200, login(nombre, clave).statusCode());
    jdbc.update(
        "UPDATE cuentas SET contrasena_hash = ? WHERE id = ?",
        hashes.generar(UUID.randomUUID().toString()),
        id);
    assertEquals(401, enviar("GET", "/api/auth/me", null, false).statusCode());
  }

  @Test
  void serializaDiezIntentosConcurrentesSinPerderElBloqueo() throws Exception {
    AtomicInteger verificaciones = new AtomicInteger();
    try (var pool = Executors.newFixedThreadPool(10)) {
      List<Future<?>> tareas = new ArrayList<>();
      for (int i = 0; i < 10; i++)
        tareas.add(
            pool.submit(
                () ->
                    assertThrows(
                        CredencialesInvalidas.class,
                        () ->
                            intentos.verificar(
                                nombre,
                                () -> {
                                  verificaciones.incrementAndGet();
                                  throw new CredencialesInvalidas();
                                }))));
      for (var tarea : tareas) tarea.get(15, TimeUnit.SECONDS);
    }
    assertEquals(5, verificaciones.get());
    assertThrows(
        CredencialesInvalidas.class,
        () ->
            intentos.verificar(
                nombre, () -> new AccesoVerificado(new Cuenta(id, nombre, true), 0)));
  }

  private void asignarRoles(String... roles) {
    jdbc.update("DELETE FROM cuenta_roles WHERE cuenta_id = ?", id);
    for (String rol : roles)
      jdbc.update("INSERT INTO cuenta_roles(cuenta_id, rol) VALUES (?, ?)", id, rol);
  }

  @Test
  void lasDieciseisCombinacionesRespetanLaAutorizacionSinHerenciaImplicita() throws Exception {
    String[] catalogo = {"REC", "CAJ", "ADM", "PRO"};
    for (int mascara = 0; mascara < 16; mascara++) {
      List<String> seleccion = new ArrayList<>();
      for (int bit = 0; bit < catalogo.length; bit++)
        if ((mascara & (1 << bit)) != 0) seleccion.add(catalogo[bit]);
      asignarRoles(seleccion.toArray(String[]::new));
      renovarCsrf();
      var acceso = login(nombre, clave);
      assertEquals(200, acceso.statusCode());
      assertEquals(seleccion.stream().sorted().toList(), read(acceso.body(), "$.roles"));
      boolean admin = seleccion.contains("ADM");
      List<String> permisos = new ArrayList<>();
      if (admin)
        permisos.addAll(List.of("CUENTAS_CONSULTAR", "CUENTAS_GESTIONAR", "ROLES_GESTIONAR"));
      if (seleccion.contains("REC")) permisos.add("PACIENTES_GESTIONAR");
      assertEquals(permisos.stream().sorted().toList(), read(acceso.body(), "$.permisos"));
      assertEquals(
          admin ? 200 : 403, enviar("GET", "/api/cuentas/" + id, null, false).statusCode());
      assertEquals(
          admin ? 404 : 403,
          enviar("GET", "/api/cuentas/" + UUID.randomUUID(), null, false).statusCode());
      assertEquals(403, enviar("GET", "/api/pacientes", null, false).statusCode());
    }
  }

  @Test
  void losCambiosDeRolSeAplicanEnLaSiguienteSolicitudConLaMismaCookie() throws Exception {
    asignarRoles("ADM");
    assertEquals(200, login(nombre, clave).statusCode());
    String original = cookie();
    assertEquals(200, enviar("GET", "/api/cuentas/" + id, null, false).statusCode());
    asignarRoles("REC", "CAJ");
    var me = enviar("GET", "/api/auth/me", null, false);
    assertEquals(200, me.statusCode());
    assertEquals(List.of("CAJ", "REC"), read(me.body(), "$.roles"));
    assertEquals(List.of("PACIENTES_GESTIONAR"), read(me.body(), "$.permisos"));
    assertEquals(403, enviar("GET", "/api/cuentas/" + id, null, false).statusCode());
    assertEquals(original, cookie());
    asignarRoles("ADM", "PRO");
    assertEquals(200, enviar("GET", "/api/cuentas/" + id, null, false).statusCode());
    assertEquals(original, cookie());
  }

  @Test
  void elClienteNoPuedeElegirSusRolesNiPermisosAlHacerLogin() throws Exception {
    asignarRoles("REC");
    String body =
        "{\"identificador\":\""
            + nombre
            + "\",\"contrasena\":\""
            + clave
            + "\",\"roles\":[\"ADM\"],\"permisos\":[\"CUENTAS_CONSULTAR\"]}";
    var acceso = enviar("POST", "/api/auth/login", body, true);
    assertEquals(200, acceso.statusCode());
    assertEquals(List.of("REC"), read(acceso.body(), "$.roles"));
    assertEquals(List.of("PACIENTES_GESTIONAR"), read(acceso.body(), "$.permisos"));
    assertEquals(403, enviar("GET", "/api/cuentas/" + id, null, false).statusCode());
  }

  @Test
  void unAdministradorConSesionVencidaTampocoPuedeLeerCuentas() throws Exception {
    asignarRoles("ADM");
    assertEquals(200, login(nombre, clave).statusCode());
    reloj.avanzar(1800);
    assertEquals(401, enviar("GET", "/api/cuentas/" + id, null, false).statusCode());
  }

  @Test
  void rechazaRolesNoReconocidosEnPostgres() {
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> jdbc.update("INSERT INTO cuenta_roles(cuenta_id, rol) VALUES (?, ?)", id, "XYZ"));
  }
}
