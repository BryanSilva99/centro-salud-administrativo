# Backend · Java y Spring Boot

Monolito modular con arquitectura hexagonal. Estado: únicamente estructura.

```text
src/main/java/pe/centrosalud/
  modules/                    Áreas del negocio
    <modulo>/domain/          Reglas y entidades en Java puro
    <modulo>/application/
      usecases/               Casos de uso
      ports/                  Contratos requeridos por los casos de uso
  adapters/inbound/http/      Futuros controladores Spring
  adapters/outbound/postgres/ Futura persistencia
  adapters/outbound/telemetry/Futura observabilidad
  bootstrap/                 Futura composición de dependencias
src/main/resources/          Futura configuración
src/test/java/pe/centrosalud/ Ubicación de pruebas futuras
src/test/resources/          Recursos de pruebas futuros
```

Los archivos `package-info.java` solo declaran y describen paquetes. No hay clases, interfaces de negocio, endpoints, anotaciones de Spring, repositorios ni métodos implementados.

Las versiones de Java y Spring Boot, la herramienta de construcción y sus dependencias se fijarán al autorizar la preparación ejecutable. No hay `pom.xml`, configuración Gradle ni aplicación de arranque en esta etapa.

Los modelos del dominio no tendrán anotaciones JPA ni dependencias de Spring. Las futuras entidades de persistencia y su traducción pertenecerán al adaptador PostgreSQL. Las transacciones se coordinarán sin introducir dependencias del framework en el dominio.
