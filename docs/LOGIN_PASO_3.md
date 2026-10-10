# Login con sesión persistida: paso 3

Actualizado el 9 de octubre de 2026. Backend implementado y verificado mediante HTTP real y PostgreSQL. No se ha implementado el formulario React ni la administración/asignación de roles. Iniciar sesión solo habilita consultar la propia sesión y registrar actividad; los endpoints de negocio, incluida la consulta administrativa de cuentas, permanecen denegados.

## Contrato HTTP

| Método y ruta | Acceso | Resultado |
|---|---|---|
| `GET /api/auth/csrf` | Público | Token CSRF y nombre de su cabecera; establece la sesión previa si es necesaria. |
| `POST /api/auth/login` | Público, exige CSRF | Recibe `identificador` y `contrasena`; devuelve ID, identificador y estado. Establece una sesión autenticada sin devolver su ID en JSON. |
| `GET /api/auth/me` | Sesión válida | Devuelve la propia cuenta. No prolonga el periodo de actividad humana. |
| `POST /api/auth/activity` | Sesión válida y CSRF | 204; registra actividad explícita. El futuro React deberá invocarlo a partir de interacción del usuario, no mediante un temporizador. |
| `POST /api/auth/logout` | Exige CSRF | 204; invalida la sesión y elimina su cookie. Repetir el logout con un token de una sesión invalidada requiere obtener un nuevo CSRF. |

Errores: 400 para solicitudes mal formadas, 401 para credenciales rechazadas, bloqueo temporal o sesión ausente/vencida, y 403 para CSRF inválido u operación prohibida. El error de credenciales no revela cuenta inexistente, estado inactivo o bloqueo. Otras rutas no se abren automáticamente por estar autenticado.

No se debe enviar el hash desde React: el usuario envía su contraseña por HTTPS y el backend la verifica. No guardar contraseñas, cookies ni tokens en logs. El desarrollo local usa HTTP con cookie Secure desactivada solo en `.env`; la configuración por defecto exige Secure para despliegue HTTPS.

## Recorrido

```text
GET csrf → cookie previa + token
POST login + cookie + cabecera X-CSRF-TOKEN
  → IniciarSesion
  → ControlIntentosPostgres: bloqueo transaccional por identificador
  → VerificarCredenciales: búsqueda + hash + cuenta activa
  → resultado con cuenta y versión de acceso
  → cambiar ID de sesión y retirar el CSRF anterior
  → guardar SecurityContext explícitamente
  → Spring Session persiste la sesión en PostgreSQL
GET csrf → obtener token nuevo tras autenticación
GET me → validar tiempos, estado y versión antes de devolver la cuenta
POST logout + CSRF nuevo → invalidar sesión; cookie antigua deja de servir
```

Se emplean [guardado explícito del contexto y estrategias de sesión de Spring Security](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html). La [protección CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) permanece habilitada también para el login y logout. No se habilita CORS abierto; React utilizará el proxy del mismo origen preparado en Vite.

## Archivos y responsabilidades

Rutas Java relativas a `backend/src/main/java/pe/centrosalud/`:

| Archivo | Función y motivo |
|---|---|
| `modules/identity/domain/IntentosLogin.java` | Regla pura: cinco fallos dentro de una ventana móvil de 15 minutos bloquean 15 minutos desde el quinto. Un intento bloqueado no prolonga el bloqueo; un éxito reinicia fallos. |
| `modules/identity/domain/VigenciaSesion.java` | Define 30 minutos sin actividad y 8 horas absolutas desde el login, con rechazo exacto al llegar al límite. |
| `modules/identity/application/usecases/IniciarSesion.java` | Coordina la verificación bajo el control de intentos. No conoce HTTP, cookies ni Spring. |
| `modules/identity/application/usecases/AccesoVerificado.java` | Lleva cuenta y versión leídas en la misma consulta de credenciales. Evita asociar una contraseña comprobada contra una versión vieja con una versión nueva obtenida después. |
| `modules/identity/application/ports/ControlIntentos.java` | Exige serializar la verificación por identificador y conservar fallos aunque el acceso sea rechazado. |
| `adapters/outbound/postgres/identity/ControlIntentosPostgres.java` | Usa una transacción independiente y bloqueo `FOR UPDATE`. Confirma el fallo antes de lanzar la excepción; así el rechazo no revierte el contador. Usa SHA-256 del identificador como clave estable, diferente del hash adaptativo de contraseñas. No se afirma que esa clave anonimice el identificador. |
| `modules/identity/application/ports/ConsultarEstadoAcceso.java` | Contrato mínimo de lectura de estado y versión; no transporta contraseñas. |
| `adapters/outbound/postgres/identity/EstadoAccesoPostgres.java` | Lee el estado vigente directamente de PostgreSQL en cada solicitud autenticada. |
| `adapters/inbound/http/identity/PrincipalSesion.java` | Identidad serializable formada solo por UUID y versión de acceso, sin contraseñas ni hashes. |
| `adapters/inbound/http/identity/ValidarSesionFilter.java` | Revalida identidad, estado, versión y tiempos antes de autorizar el acceso. Una sesión inválida se invalida y se limpia el contexto. Se registra una sola vez, dentro de Spring Security. |
| `adapters/inbound/http/identity/LoginRequest.java` | Entrada validada sin `toString()` que imprima contraseñas. La política cuenta hasta 128 puntos Unicode; el DTO admite hasta 256 unidades UTF-16 para no rechazar indebidamente caracteres suplementarios. |
| `adapters/inbound/http/identity/AuthController.java` | Traduce HTTP y establece la sesión con las herramientas de Spring Security. No implementa SQL ni el algoritmo criptográfico. |
| `bootstrap/SecurityConfiguration.java` | Expone solo las rutas necesarias, configura CSRF, guardado explícito, renovación de ID, logout y errores genéricos; desactiva formulario y HTTP Basic automáticos. |
| `bootstrap/IdentityConfiguration.java` | Conecta casos de uso, puertos y un reloj intercambiable para probar vencimientos sin esperar minutos u horas. |

## Migración V4

`backend/src/main/resources/db/migration/V4__control_acceso.sql` añade:

- `cuentas.version_acceso` y un trigger que la incrementa al cambiar estado o hash. Desactivar y reactivar una cuenta sigue invalidando su sesión anterior. El futuro restablecimiento deberá cambiar el hash y aprovechará esta revocación; el restablecimiento completo aún no está implementado.
- `intentos_login`, con fechas de fallos y fin del bloqueo. Se persiste también para identificadores ausentes, para aplicar el mismo comportamiento. Los contadores sobreviven al proceso de aplicación; se verificó su persistencia entre solicitudes, no se hizo una prueba independiente de reinicio del servidor.

Las tablas de Spring Session ya estaban preparadas por V1; ahora se usan para guardar las sesiones autenticadas. Antes de desplegar habrá que definir retención/limpieza de identificadores antiguos en `intentos_login` y controles de tráfico generales. Este paso implementa el límite por cuenta/identificador de RNF-02, no una defensa integral frente a denegación de servicio.

## Verificación realizada

Desde `backend/`, con PostgreSQL local iniciado:

```bash
RUN_POSTGRES_TESTS=true ./mvnw test
```

37 pruebas pasaron sin fallos ni omisiones. Las nuevas pruebas utilizan un servidor HTTP real en puerto aleatorio, cookies reales y sesiones en PostgreSQL. Se verificó:

- Login, consulta propia, persistencia, rotación de ID y ausencia de secretos en la respuesta.
- CSRF requerido para login/logout y renovación del token después de autenticar.
- Logout y rechazo de reutilización de la cookie anterior.
- Cinco fallos, bloqueo incluso con contraseña correcta, vencimiento y reinicio por éxito.
- Diez intentos concurrentes: solo los cinco primeros llegan a verificar; el resto encuentra el bloqueo.
- Ventana móvil de fallos y límites exactos de tiempo.
- Consultar `me` no renueva actividad; actividad explícita sí renueva los 30 minutos, pero nunca las 8 horas.
- Revocación tras cambio de contraseña o desactivación/reactivación.
- Rutas de negocio denegadas incluso con sesión autenticada.

Las cuentas, contraseñas aleatorias, intentos y sesiones de prueba se limpian. No se creó una cuenta permanente de demostración. Las migraciones sí permanecen aplicadas. El servidor HTTP de pruebas se detiene al finalizar.

## Qué es general y qué pertenece al proyecto

Generales: usar HTTPS, proteger solicitudes basadas en cookies con CSRF, renovar la sesión al autenticar, invalidarla al salir, evitar errores que revelen cuentas y persistir correctamente el resultado de intentos fallidos.

Decisiones de este proyecto: sesión JDBC en PostgreSQL, 30 minutos/8 horas, cinco fallos/15 minutos, identificador sensible a mayúsculas, uso explícito de actividad, versión de acceso en base y organización hexagonal. Sus valores y mecanismos no son universales.

## Siguiente paso

Diseñar y conectar la pantalla React con estos contratos y preparar de forma controlada una cuenta local de prueba. La asignación y actualización de roles, permisos por recurso, auditoría de cambios de acceso y recuperación de contraseña siguen pendientes. Por ello no se marca RF-02, RF-03 ni toda la matriz de autorización como cumplidos.
