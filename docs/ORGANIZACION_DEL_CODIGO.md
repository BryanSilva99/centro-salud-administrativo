# Organización del código

Estado: estructura técnica, identidad y dos iteraciones de appointments implementadas. Ver [recorrido de Cuenta](CUENTA_PASO_1.md) y [módulo de citas](../backend/src/main/java/pe/centrosalud/modules/appointments/README.md).

Backend en Java con Spring Boot y frontend en React, según la elección del usuario. El dominio se mantendrá independiente de Spring y JPA.

## Vista general

```text
backend/
  src/main/java/pe/centrosalud/
    modules/
      identity/             Identidad y acceso
      administration/       Pacientes y recursos del centro
      appointments/         Citas, admisión y atención
      billing/              Caja y movimientos económicos
      reporting/            Consultas e indicadores
      audit/                Auditoría del negocio
    adapters/
      inbound/http/         Entrada HTTP prevista con Spring Boot
      outbound/postgres/    Persistencia y transacciones
      outbound/telemetry/   Instrumentación técnica
    bootstrap/              Conexión de contratos y adaptadores
  src/test/java/pe/centrosalud/
    unit/
    integration/
    architecture/
frontend/
  src/
    app/                    Composición y navegación
    features/               Áreas funcionales correspondientes al backend
    shared/ui/              Componentes visuales reutilizables
    shared/api/             Cliente HTTP
    shared/lib/             Utilidades de presentación
  tests/
infra/
  deployment/
  observability/
docs/
```

## Organización dentro de cada módulo

Cada módulo contiene `domain/`, `application/usecases/` y `application/ports/`.

| Ubicación | Qué contendrá | Qué no debe contener |
|---|---|---|
| domain | Entidades, estados y reglas del módulo | Spring Boot, SQL, React, solicitudes HTTP |
| application/usecases | Coordinación de autorización, reglas, transacción y auditoría | Detalles del framework o del controlador de base de datos |
| application/ports | Contratos necesarios para ejecutar los casos de uso | Implementaciones concretas PostgreSQL o SigNoz |
| adapters/inbound/http | Rutas, esquemas de entrada y traducción de errores | Reglas de negocio duplicadas |
| adapters/outbound/postgres | Repositorios, restricciones y unidad de trabajo | Decisiones de interfaz o permisos basados en botones |
| adapters/outbound/telemetry | Exportación técnica sin información sensible | Auditoría de negocio como sustituto de su registro transaccional |
| bootstrap | Ensamblado de dependencias y configuración | Reglas o procesos del negocio |

## Dirección de dependencias

```mermaid
flowchart LR
    HTTP[Adaptador HTTP] --> UC[Casos de uso]
    UC --> D[Dominio]
    UC --> P[Puertos de aplicación]
    PG[Adaptador PostgreSQL] --> P
    OT[Adaptador de telemetría] --> P
```

Las flechas indican dependencias del código, no el orden temporal de una solicitud. La composición conecta implementaciones a contratos. Los módulos se ejecutarán en un mismo backend y compartirán una unidad de trabajo cuando la operación requiera atomicidad; esta estructura no crea microservicios.

## Correspondencia con el ADD

| Elemento ADD | Ubicación prevista |
|---|---|
| E-01 · React | frontend/src |
| E-02 · Identidad | modules/identity |
| E-03 · Gestión administrativa | modules/administration |
| E-04 · Citas y atención | modules/appointments |
| E-05 · Caja | modules/billing |
| E-06 · Consultas y auditoría | modules/reporting y modules/audit |
| E-07 · PostgreSQL | adapters/outbound/postgres |
| E-08 · SigNoz | adapters/outbound/telemetry e infra/observability |

Reportes y auditoría se separan en carpetas porque tienen responsabilidades diferentes, aunque el ADD los agrupe en E-06. Los módulos solo deben colaborar mediante contratos explícitos; no escribirán directamente en tablas ajenas.

## Ejemplo de recorrido futuro

Una reserva entra por HTTP, invoca un caso de uso de `appointments`, comprueba reglas del dominio y utiliza puertos para persistencia y disponibilidad. PostgreSQL implementa las garantías de concurrencia mediante rangos y restricciones de solapamiento. El catálogo y la autorización actuales son fixtures locales reemplazables; la integración con los módulos externos y la auditoría global quedan pendientes.

## Estado y límite actual

Los archivos `package-info.java` solo declaran paquetes Java y documentan su responsabilidad. Los `.gitkeep` permiten conservar carpetas vacías en Git. Cuenta tiene dominio, caso de uso, puerto, persistencia JPA, controlador de consulta y pruebas. Se implementó login HTTP con sesiones, límite de intentos y pruebas; ver [paso 3](LOGIN_PASO_3.md). `appointments` contiene dominio independiente, puertos, adaptador PostgreSQL, API autenticada, fixtures locales opt-in y el workspace React de agenda, llegada, atención, cierres y dashboard. Las fixtures no representan datos permanentes de pacientes o personal. Vista semanal, reaperturas, caja, auditoría global y RF-26 siguen pendientes. Dependencias y comandos: [Inicio del desarrollo](INICIO_DESARROLLO.md).

Antes de comenzar la implementación, el usuario decidirá qué parte desarrollar. No se presupone autorización por la fecha de entrega.
