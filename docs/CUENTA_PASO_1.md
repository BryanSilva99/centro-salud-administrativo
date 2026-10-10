# Cuenta: primer recorrido entre capas

Se implementa únicamente consultar una cuenta por UUID. Es una base parcial de identidad, no el cumplimiento completo de RF-01 ni RF-02. No hay creación, edición, desactivación, credenciales, roles, sesiones autenticadas ni interfaz React.

## Recorrido de ejecución

```text
GET /api/cuentas/{id}
  → filtros de seguridad (actualmente deniegan todo)
  → CuentaController
  → ConsultarCuenta
  → ConsultarCuentas (contrato)
  → ConsultarCuentasPostgres
  → CuentaJpaRepository
  → PostgreSQL: cuentas
```

Para estudiar el recorrido se usan pruebas controladas: las pruebas aisladas del controlador no cargan los filtros; otras pruebas cargan la cadena real y exigen 403. No se desactiva seguridad en la aplicación.

## Archivos y motivos

Todos los archivos Java de producción parten de `backend/src/main/java/pe/centrosalud/`.

| Archivo | Responsabilidad y motivo |
|---|---|
| `modules/identity/domain/Cuenta.java` | Representación inmutable con ID, identificador y estado. Valida que el ID y el identificador existan. No sabe de Spring, HTTP ni SQL. Una cuenta inactiva sigue siendo un objeto válido; estar activa no equivale a autenticarse. |
| `modules/identity/application/ports/ConsultarCuentas.java` | Interfaz de consulta por ID. `Optional` expresa que puede no existir. El caso de uso conoce este contrato, no PostgreSQL. |
| `modules/identity/application/usecases/ConsultarCuenta.java` | Solicita la cuenta y convierte su ausencia en un resultado de aplicación explícito. Es deliberadamente pequeño; no inventa reglas para justificar una capa. |
| `modules/identity/application/usecases/CuentaNoEncontrada.java` | Excepción independiente de HTTP. El dominio y la aplicación no deciden códigos de respuesta web. |
| `adapters/outbound/postgres/identity/CuentaJpaEntity.java` | Representa la fila para JPA. `@Entity`, `@Table`, `@Id` y `@Column` describen el mapeo. El constructor protegido sin argumentos permite a JPA reconstruirla. `toDomain()` devuelve Java puro. |
| `adapters/outbound/postgres/identity/CuentaJpaRepository.java` | Spring Data implementa `findById` automáticamente. Extiende `Repository` y expone solo lectura; no publica operaciones de escritura innecesarias. |
| `adapters/outbound/postgres/identity/ConsultarCuentasPostgres.java` | Implementa el puerto mediante JPA y convierte la fila al dominio. `@Transactional(readOnly = true)` delimita esta consulta; no sustituye permisos de acceso. |
| `bootstrap/IdentityConfiguration.java` | Un método `@Bean` construye el caso de uso con el adaptador que implementa su puerto. Así la aplicación no necesita `@Service` ni dependencia de Spring. |
| `adapters/inbound/http/identity/CuentaController.java` | Traduce UUID de la URL, invoca el caso de uso y devuelve un DTO. Su manejador traduce cuenta inexistente a 404. No consulta directamente la base. |
| `backend/src/main/resources/db/migration/V2__cuentas.sql` | Flyway crea la tabla con UUID como clave primaria, identificador único y no vacío, y estado obligatorio. No precarga cuentas. |

La dirección de dependencia del código es distinta del recorrido: el adaptador PostgreSQL depende del puerto definido por la aplicación. La aplicación no importa al adaptador. Esto es inversión de dependencias.

El DTO `CuentaResponse` mantiene independiente el contrato JSON: añadir un atributo interno al dominio no lo publica automáticamente. El `record` de dominio no contiene contraseñas ni hashes.

## Contrato y límites

Ruta implementada: `GET /api/cuentas/{id}`.

En pruebas aisladas del controlador: 200 con `id`, `identificador`, `activa`; 404 si no existe; 400 si el parámetro no es un UUID. En la aplicación, la política actual deniega todas las solicitudes con 403 antes de ejecutar el controlador. La autorización administrativa se añadirá junto con autenticación, sin considerar este endpoint listo para usuarios.

La migración usa igualdad exacta para la unicidad del identificador. No se ha fijado todavía si el ingreso será con correo o nombre de usuario, ni su normalización; eso deberá resolverse antes de implementar creación o login. No hay endpoint público para enumerar cuentas por identificador.

## Qué es general y qué es una elección

- Validar datos y garantizar unicidad en la base son prácticas generales. Una comprobación previa en Java no evitaría duplicados concurrentes.
- UUID y `record` son elecciones técnicas; no requisitos universales.
- Separar dominio, aplicación, puertos y adaptadores corresponde a nuestra arquitectura hexagonal; no es la única arquitectura posible.
- Spring Data, JPA y Flyway son herramientas de este stack. El dominio podría conservarse al cambiar el adaptador.
- Conservar cuentas inactivas y restringir acceso responden a los requisitos de este sistema. Los roles y la protección del último administrador aún no se implementan.

## Verificación

Desde `backend/`, `./mvnw test` ejecuta las pruebas aisladas y omite las de PostgreSQL. Con la base local iniciada y `.env` configurado:

```bash
RUN_POSTGRES_TESTS=true ./mvnw test
```

Las pruebas de integración aplican migraciones a la base configurada. Las filas de prueba se insertan dentro de transacciones que se revierten; las migraciones permanecen. Usar solo una base de desarrollo o de pruebas.

Se comprueban invariantes del dominio, ausencia de cuenta, traducción HTTP, recorrido real controlador–caso de uso–PostgreSQL, restricción de unicidad y cierre de acceso anónimo y de una identidad simulada. Las pruebas con acceso simulado no implementan login.
