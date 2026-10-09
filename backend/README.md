# Backend · Java y Spring Boot

Monolito modular con arquitectura hexagonal. Estado: estructura, dependencias y arranque técnico preparados.

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

Los archivos `package-info.java` solo declaran y describen paquetes. Se añadieron únicamente el arranque de Spring Boot y una configuración preventiva que deniega acceso. No hay entidades, casos de uso, endpoints ni repositorios implementados.

Base: Java 21, Spring Boot 4.1.1 y Maven Wrapper 3.9.16. Ver [guía de preparación](../docs/INICIO_DESARROLLO.md) para configurar PostgreSQL y ejecutar el proyecto.

Los modelos del dominio no tendrán anotaciones JPA ni dependencias de Spring. Las futuras entidades de persistencia y su traducción pertenecerán al adaptador PostgreSQL. Las transacciones se coordinarán sin introducir dependencias del framework en el dominio.
