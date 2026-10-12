# Backend · Java y Spring Boot

Monolito modular con arquitectura hexagonal. Estado: identidad, administración, citas y caja integradas; ver [validación](../docs/VALIDACION_INTEGRADA.md).

```text
src/main/java/pe/centrosalud/
  modules/                    Áreas del negocio
    <modulo>/domain/          Reglas y entidades en Java puro
    <modulo>/application/
      usecases/               Casos de uso
      ports/                  Contratos requeridos por los casos de uso
  adapters/inbound/http/      Controladores Spring por módulo
  adapters/outbound/postgres/ Persistencia y transacciones
  adapters/outbound/telemetry/Futura observabilidad
  bootstrap/                 Composición de dependencias
src/main/resources/          Configuración y migraciones
src/test/java/pe/centrosalud/ Pruebas unitarias e integración
src/test/resources/          Recursos de pruebas
```

Los archivos `package-info.java` solo declaran y describen paquetes. Se añadieron el arranque de Spring Boot, la sesión HTTP y el recorrido de appointments para agenda, programación, reprogramación, llegada, atención y dashboard. El dominio de citas sigue independiente de Spring y JPA; sus catálogos y permisos reales se conectan por contratos de administración e identidad. Ver [explicación](../docs/CUENTA_PASO_1.md) y la [documentación de appointments](src/main/java/pe/centrosalud/modules/appointments/README.md).

Base: Java 21, Spring Boot 4.1.1 y Maven Wrapper 3.9.16. Ver [guía de preparación](../docs/INICIO_DESARROLLO.md) para configurar PostgreSQL y ejecutar el proyecto.

`appointments` implementa agenda diaria, programación, reprogramación, llegada, espera, atención, cancelación, ausencia, retiro de espera y dashboard operativo. Para probarlo, registra pacientes, recursos y turnos desde administración y vincula una cuenta PRO a su ficha. Los roles se verifican en cada solicitud. Vista semanal, reaperturas, auditoría global y RF-26 siguen pendientes. Caja está integrada; consultar sus límites en [la integración económica](../docs/INTEGRACION_CAJA.md).

Los modelos del dominio no tendrán anotaciones JPA ni dependencias de Spring. Las futuras entidades de persistencia y su traducción pertenecerán al adaptador PostgreSQL. Las transacciones se coordinarán sin introducir dependencias del framework en el dominio.

Para ejecutar el conjunto con frontend y PostgreSQL en contenedores, ver [Docker](../docs/DOCKER.md).
