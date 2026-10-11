package pe.centrosalud.integration;

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
import pe.centrosalud.modules.administration.application.ports.Servicios;
import pe.centrosalud.modules.administration.application.usecases.ErrorServicios;
import pe.centrosalud.modules.administration.application.usecases.GestionarServicios;
import pe.centrosalud.modules.administration.application.usecases.GestionarServicios.DatosModalidad;
import pe.centrosalud.modules.administration.domain.Servicio;
import pe.centrosalud.modules.identity.application.ports.HashContrasenas;
import static org.junit.jupiter.api.Assertions.*;
import static com.jayway.jsonpath.JsonPath.read;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties="server.servlet.session.cookie.secure=false")
@EnabledIfEnvironmentVariable(named="RUN_POSTGRES_TESTS",matches="true")
class ServiciosHttpTest {
    static final String SCHEMA="test_servicios_"+UUID.randomUUID().toString().replace("-", "");
    @DynamicPropertySource static void schema(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.default-schema", () -> SCHEMA);
        registry.add("spring.datasource.hikari.schema", () -> SCHEMA);
    }
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired HashContrasenas hashes;
    @Autowired GestionarServicios gestionar;
    @Autowired Servicios servicios;
    String password, csrf; HttpClient client;

    @BeforeEach void preparar() { password=UUID.randomUUID().toString(); }
    @AfterEach void limpiar() {
        jdbc.execute("TRUNCATE auditoria_servicios,servicio_modalidades,servicios,spring_session,intentos_login,cuentas CASCADE");
    }
    @AfterAll static void quitarSchema(@Autowired JdbcTemplate jdbc) { jdbc.execute("DROP SCHEMA "+SCHEMA+" CASCADE"); }

    UUID cuenta(String... roles) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO cuentas(id,identificador,activa,contrasena_hash) VALUES (?,?,true,?)",id,id.toString(),hashes.generar(password));
        for(String rol:roles) jdbc.update("INSERT INTO cuenta_roles(cuenta_id,rol) VALUES (?,?)",id,rol);
        return id;
    }
    UUID ingresar(String... roles) throws Exception {
        UUID id=cuenta(roles);
        client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        token();
        assertEquals(200,send("POST","/api/auth/login","{\"identificador\":\""+id+"\",\"contrasena\":\""+password+"\"}",true).statusCode());
        token();
        return id;
    }
    HttpResponse<String> send(String method,String path,String body,boolean token) throws Exception {
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));
        if(token)b.header("X-CSRF-TOKEN",csrf);
        if(body!=null)b.header("Content-Type","application/json");
        return client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    void token() throws Exception { csrf=read(send("GET","/api/auth/csrf",null,false).body(),"$.token"); }
    static String nuevo(String nombre,String tarifa) {
        return "{\"nombre\":\""+nombre+"\",\"duracionMinutos\":30,\"modalidades\":[{\"nombre\":\"Particular\",\"conCargo\":true,\"tarifa\":\""+tarifa
                +"\",\"activa\":true},{\"nombre\":\"Campaña\",\"conCargo\":false,\"tarifa\":\"0\",\"activa\":true}]}";
    }
    HttpResponse<String> crear(String nombre) throws Exception { return send("POST","/api/servicios",nuevo(nombre,"40.00"),true); }
    String modalidad(Servicio servicio,String nombre) {
        return servicio.modalidades().stream().filter(m->m.nombre().equals(nombre)).findFirst().orElseThrow().id().toString();
    }
    String edicion(Servicio s,int duracion,boolean activo,String tarifaParticular,boolean particularActiva,long version) {
        return "{\"nombre\":\""+s.nombre()+"\",\"duracionMinutos\":"+duracion+",\"activo\":"+activo+",\"version\":"+version+",\"modalidades\":["
                +"{\"id\":\""+modalidad(s,"Particular")+"\",\"nombre\":\"Particular\",\"conCargo\":true,\"tarifa\":\""+tarifaParticular+"\",\"activa\":"+particularActiva+"},"
                +"{\"id\":\""+modalidad(s,"Campaña")+"\",\"nombre\":\"Campaña\",\"conCargo\":false,\"tarifa\":\"0.00\",\"activa\":true}]}";
    }
    Servicio cargar(String respuesta) { return servicios.buscarPorId(UUID.fromString(read(respuesta,"$.id"))).orElseThrow(); }
    int contar(String tabla) { return jdbc.queryForObject("SELECT count(*) FROM "+tabla,Integer.class); }

    @Test void administradorCreaListaYConsultaConAuditoria() throws Exception {
        UUID adm=ingresar("ADM");
        var respuesta=crear("Consulta general");
        assertEquals(201,respuesta.statusCode(),respuesta.body());
        assertEquals(0,(Integer)read(respuesta.body(),"$.version"));
        assertTrue((Boolean)read(respuesta.body(),"$.activo"));
        var servicio=cargar(respuesta.body());
        assertEquals(Set.of(new BigDecimal("40.00"),new BigDecimal("0.00")),
                Set.copyOf(servicio.modalidades().stream().map(m->m.tarifa()).toList()));
        assertEquals(201,crear("Ecografía").statusCode());
        var listado=send("GET","/api/servicios",null,false);
        assertEquals(200,listado.statusCode());
        assertEquals(List.of("Consulta general","Ecografía"),read(listado.body(),"$[*].nombre"));
        assertEquals(200,send("GET","/api/servicios/"+servicio.id(),null,false).statusCode());
        assertEquals(404,send("GET","/api/servicios/"+UUID.randomUUID(),null,false).statusCode());
        var auditoria=jdbc.queryForMap("SELECT actor_id,tipo,anterior,nuevo::text AS nuevo FROM auditoria_servicios WHERE servicio_id=?",servicio.id());
        assertEquals(adm,auditoria.get("actor_id"));
        assertEquals("CREACION",auditoria.get("tipo"));
        assertNull(auditoria.get("anterior"));
        assertTrue(auditoria.get("nuevo").toString().contains("\"tarifa\": 40.00"),auditoria.get("nuevo").toString());
    }

    @Test void editarCambiaTarifaDuracionYEstadoConVersionYAuditoria() throws Exception {
        ingresar("ADM");
        var servicio=cargar(crear("Consulta").body());
        var editado=send("PUT","/api/servicios/"+servicio.id(),edicion(servicio,45,true,"45.50",true,0),true);
        assertEquals(200,editado.statusCode(),editado.body());
        assertEquals(1,(Integer)read(editado.body(),"$.version"));
        assertEquals(45,(Integer)read(editado.body(),"$.duracionMinutos"));
        var obsoleta=send("PUT","/api/servicios/"+servicio.id(),edicion(servicio,60,true,"50",true,0),true);
        assertEquals(409,obsoleta.statusCode());
        assertEquals("CONFLICTO",read(obsoleta.body(),"$.code"));
        var actual=servicios.buscarPorId(servicio.id()).orElseThrow();
        assertEquals(45,actual.duracionMinutos());
        // Desactivar una modalidad conserva la fila; desactivar el servicio también.
        assertEquals(200,send("PUT","/api/servicios/"+servicio.id(),edicion(actual,45,false,"45.50",false,1),true).statusCode());
        actual=servicios.buscarPorId(servicio.id()).orElseThrow();
        assertFalse(actual.activo());
        assertEquals(2,actual.modalidades().size());
        assertEquals(3,contar("auditoria_servicios"));
        var cambio=jdbc.queryForMap("SELECT anterior::text AS anterior,nuevo::text AS nuevo FROM auditoria_servicios WHERE tipo='ACTUALIZACION' ORDER BY fecha,id LIMIT 1");
        assertTrue(cambio.get("anterior").toString().contains("\"tarifa\": 40.00"));
        assertTrue(cambio.get("nuevo").toString().contains("\"tarifa\": 45.50"));
        // Guardar sin cambios no incrementa la versión ni audita.
        assertEquals(200,send("PUT","/api/servicios/"+servicio.id(),edicion(actual,45,false,"45.5",false,2),true).statusCode());
        assertEquals(2,servicios.buscarPorId(servicio.id()).orElseThrow().version());
        assertEquals(3,contar("auditoria_servicios"));
    }

    @Test void rechazaQuitarModalidadesYNombresDuplicados() throws Exception {
        ingresar("ADM");
        var servicio=cargar(crear("Consulta").body());
        String soloUna="{\"nombre\":\"Consulta\",\"duracionMinutos\":30,\"activo\":true,\"version\":0,\"modalidades\":[{\"id\":\""
                +modalidad(servicio,"Particular")+"\",\"nombre\":\"Particular\",\"conCargo\":true,\"tarifa\":\"40\",\"activa\":true}]}";
        var omitida=send("PUT","/api/servicios/"+servicio.id(),soloUna,true);
        assertEquals(400,omitida.statusCode());
        assertEquals("MODALIDAD_INVALIDA",read(omitida.body(),"$.code"));
        var duplicado=crear("CONSULTA");
        assertEquals(409,duplicado.statusCode());
        assertEquals("NOMBRE_DUPLICADO",read(duplicado.body(),"$.code"));
        assertEquals(1,contar("servicios"));
    }

    @Test void diezEdicionesConcurrentesConLaMismaVersionSoloAplicanUna() throws Exception {
        UUID adm=cuenta("ADM");
        var servicio=gestionar.crear(adm,"Consulta",30,List.of(new DatosModalidad(null,"Particular",true,new BigDecimal("40"),true)));
        UUID particular=servicio.modalidades().getFirst().id();
        var inicio=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(10)) {
            List<Future<String>> tareas=new ArrayList<>();
            for(int i=0;i<10;i++) { var tarifa=new BigDecimal(41+i); tareas.add(pool.submit(() -> {
                inicio.await();
                try { gestionar.actualizar(adm,servicio.id(),"Consulta",30,true,List.of(new DatosModalidad(particular,"Particular",true,tarifa,true)),0); return "OK"; }
                catch(ErrorServicios e) { return e.codigo().name(); }
            })); }
            inicio.countDown();
            List<String> resultados=new ArrayList<>();
            for(var tarea:tareas) resultados.add(tarea.get(15,TimeUnit.SECONDS));
            assertEquals(1,Collections.frequency(resultados,"OK"),resultados.toString());
            assertEquals(9,Collections.frequency(resultados,"CONFLICTO"),resultados.toString());
        }
        assertEquals(1,servicios.buscarPorId(servicio.id()).orElseThrow().version());
        assertEquals(2,contar("auditoria_servicios"));
    }

    @Test void diezAltasConcurrentesDelMismoNombreCreanUnSoloServicio() throws Exception {
        UUID adm=cuenta("ADM");var inicio=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(10)) {
            List<Future<String>> tareas=new ArrayList<>();
            for(int i=0;i<10;i++) { String nombre=i%2==0?"Consulta":"consulta"; tareas.add(pool.submit(() -> {
                inicio.await();
                try { gestionar.crear(adm,nombre,30,List.of(new DatosModalidad(null,"Particular",true,new BigDecimal("40"),true))); return "OK"; }
                catch(ErrorServicios e) { return e.codigo().name(); }
            })); }
            inicio.countDown();
            List<String> resultados=new ArrayList<>();
            for(var tarea:tareas) resultados.add(tarea.get(15,TimeUnit.SECONDS));
            assertEquals(1,Collections.frequency(resultados,"OK"),resultados.toString());
            assertEquals(9,Collections.frequency(resultados,"NOMBRE_DUPLICADO"),resultados.toString());
        }
        assertEquals(1,contar("servicios"));
        assertEquals(1,contar("servicio_modalidades"));
        assertEquals(1,contar("auditoria_servicios"));
    }

    @Test void falloDeAuditoriaRevierteAltaYEdicion() throws Exception {
        ingresar("ADM");
        var servicio=cargar(crear("Consulta").body());
        jdbc.execute("ALTER TABLE auditoria_servicios ADD CONSTRAINT simular_fallo CHECK(false) NOT VALID");
        try {
            assertEquals(503,crear("Ecografía").statusCode());
            assertEquals(1,contar("servicios"));
            assertEquals(2,contar("servicio_modalidades"));
            assertEquals(503,send("PUT","/api/servicios/"+servicio.id(),edicion(servicio,60,true,"99",true,0),true).statusCode());
            assertEquals(servicio,servicios.buscarPorId(servicio.id()).orElseThrow());
        } finally { jdbc.execute("ALTER TABLE auditoria_servicios DROP CONSTRAINT simular_fallo"); }
    }

    @Test void rechazaDatosInvalidos() throws Exception {
        ingresar("ADM");
        for(String body:List.of(nuevo("Consulta","-1"), nuevo("Consulta","40.555"), nuevo("Consulta","0"), nuevo("Consulta","abc"),
                nuevo("","40"), nuevo("Consulta","40").replace("\"duracionMinutos\":30","\"duracionMinutos\":0"),
                nuevo("Consulta","40").replace("\"duracionMinutos\":30","\"duracionMinutos\":481"),
                "{\"nombre\":\"Consulta\",\"duracionMinutos\":30,\"modalidades\":[]}", "{\"nombre\":")) {
            assertEquals(400,send("POST","/api/servicios",body,true).statusCode(),body);
        }
        assertEquals(0,contar("servicios"));
    }

    @Test void soloAdministracionGestionaServicios() throws Exception {
        UUID adm=cuenta("ADM");
        var servicio=gestionar.crear(adm,"Consulta",30,List.of(new DatosModalidad(null,"Particular",true,new BigDecimal("40"),true)));
        String editar="/api/servicios/"+servicio.id();
        for(String rol:List.of("REC","CAJ","PRO","")) {
            if(rol.isEmpty()) ingresar(); else ingresar(rol);
            assertEquals(403,send("GET","/api/servicios",null,false).statusCode(),rol);
            assertEquals(403,send("GET",editar,null,false).statusCode(),rol);
            assertEquals(403,crear("Ecografía").statusCode(),rol);
            assertEquals(403,send("PUT",editar,"{}",true).statusCode(),rol);
        }
        assertEquals(1,contar("servicios"));
        ingresar("ADM","REC");
        assertEquals(201,crear("Ecografía").statusCode());
    }

    @Test void exigeSesionCsrfYPermisosVigentes() throws Exception {
        client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        token();
        assertEquals(401,send("GET","/api/servicios",null,false).statusCode());
        assertEquals(401,crear("Consulta").statusCode());
        UUID adm=ingresar("ADM");
        assertEquals(403,send("POST","/api/servicios",nuevo("Consulta","40"),false).statusCode());
        jdbc.update("DELETE FROM cuenta_roles WHERE cuenta_id=?",adm);
        assertEquals(403,crear("Consulta").statusCode());
        UUID otra=ingresar("ADM");
        jdbc.update("UPDATE cuentas SET activa=false WHERE id=?",otra);
        assertEquals(401,crear("Consulta").statusCode());
        assertEquals(0,contar("servicios"));
    }
}
