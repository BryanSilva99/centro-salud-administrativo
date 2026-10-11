# Gestión de cuentas y roles — avance RF-02

Actualizado: 10 de octubre de 2026. **RF-02 implementado:** creación, edición, activación/desactivación de cuentas, asignación de roles, vínculo con una ficha profesional, directorio e historial. RF-03 (cambio/restablecimiento de contraseña) es un requisito distinto y continúa fuera de este alcance.

El registro básico de fichas permite usar el vínculo sin datos ficticios precargados ni IDs introducidos a ciegas. No equivale al RF-07 completo: servicios habilitados, disponibilidad y desactivación con revisión de citas corresponden al módulo de profesionales.

## Uso

1. Ingresar en la aplicación local con una cuenta ADM y abrir **Administrar cuentas y roles**.
2. Elegir **Nueva cuenta**, introducir identificador, contraseña inicial y confirmación, y seleccionar los roles. Al guardar, se crea activa y se abre su detalle.
3. Para editar, buscar y seleccionar la cuenta. **Guardar cuenta** actualiza identificador y estado; **Guardar roles** modifica sus roles. Son operaciones separadas y cada una actualiza la versión del registro.
4. Desmarcar **Cuenta activa** y guardar para desactivar. La interfaz avisa del efecto sobre las sesiones. Volver a marcarla y guardar reactiva el acceso, pero requiere un nuevo login.
5. En una cuenta con rol PRO, buscar o registrar una ficha básica y pulsar **Guardar vínculo**. Seleccionar **Sin ficha vinculada** para desvincular; elegir otra ficha para reemplazar.
6. Consultar **Historial de la cuenta** para altas/cambios de identificador/estado y **Historial de cambios** para modificaciones posteriores de roles.

La edición no cambia contraseñas. La contraseña inicial cumple la política de 12–128 caracteres, no se devuelve ni se conserva en el navegador. Se almacena únicamente su hash PBKDF2 con sal usando el componente existente. Esta credencial inicial no es un enlace ni una clave temporal de un solo uso: ese flujo pertenece al RF-03 pendiente.

El identificador admite letras, números, `.`, `_`, `-` y `@`, hasta 254 caracteres. Se recortan espacios externos; no se permiten espacios interiores. La unicidad y el login distinguen mayúsculas/minúsculas, manteniendo la semántica existente. Las cuentas son identidades de acceso, no fichas personales: la ficha básica del profesional se registra en el módulo administrativo y se consulta mediante su contrato público.

## Cómo funciona y por qué

| Capa | Archivos | Responsabilidad |
|---|---|---|
| React | `FormulariosCuenta.tsx`, `RolesPanel.tsx`, `api/cuentas.ts` | Capturar y confirmar datos, mostrar estado/errores/historial y llamar al servidor con CSRF. |
| Entrada HTTP | `GestionCuentasController` | Validar formato, obtener el autor de la sesión y traducir resultados a HTTP. El cliente no elige quién figura como autor. |
| Aplicación | `GestionarCuentas` | Autorizar ADM, validar datos, detectar ediciones obsoletas e impedir desactivar al último administrador activo. Sin dependencia de Spring ni SQL. |
| Puertos | `UnidadAcceso`, `HistorialCuentas` | Expresar las operaciones transaccionales y de consulta que necesita la aplicación. |
| Persistencia | `GestionAccesoPostgres`, migración V7 | Guardar cuentas y auditoría en una transacción y hacer cumplir la unicidad en PostgreSQL. |
| Composición | `IdentityConfiguration`, `SecurityConfiguration` | Conectar implementaciones y restringir rutas al permiso CUENTAS_GESTIONAR, exclusivo de ADM. |

`UnidadAcceso` y `GestionAccesoPostgres` reemplazan los nombres anteriores `UnidadRoles` y `GestionRolesPostgres`: ahora coordinan tanto cuentas como roles. Ambos cambios comparten el mismo bloqueo de administración. Separarlos en bloqueos diferentes permitiría desactivar un administrador al mismo tiempo que se retira el rol del otro y dejar al sistema sin ninguno.

Se reutiliza `version_roles` como versión de edición de acceso: V7 también la incrementa al cambiar identificador o estado. Así, editar una cuenta y modificar sus roles simultáneamente genera un conflicto explícito; el usuario debe recargar antes de guardar de nuevo.

La desactivación incrementa `version_acceso`, mediante el mecanismo existente, y el filtro rechaza sesiones con versiones antiguas en la siguiente solicitud protegida. Reactivar vuelve a cambiar esa versión: una sesión anterior no recupera validez aunque no se haya utilizado durante la desactivación.

Son prácticas aplicables a otros proyectos: transacciones, validación en servidor, hash, control de concurrencia y auditoría. Son decisiones del proyecto: cuatro roles, autorización exclusiva de ADM, conservación del último administrador y la dependencia con las fichas profesionales.

## Persistencia y auditoría

- Las migraciones V7–V8 se añaden sin modificar migraciones ya aplicadas.
- No hay eliminación física de cuentas. Desactivar conserva sus IDs, roles e historial.
- La auditoría registra autor, destinatario, fecha, identificador/estado anteriores y nuevos, y roles en el momento del cambio. Nunca almacena contraseñas ni hashes.
- Si falla la auditoría, se revierte el alta o edición completa.
- Repetir una actualización sin cambios no genera registros artificiales.
- El alta no se reintenta automáticamente. La restricción única evita duplicar un mismo identificador; ante una respuesta de red incierta, la interfaz indica comprobar primero el directorio.
- La garantía del último administrador se aplica a los casos de uso de la aplicación; una escritura SQL manual la elude y no es un flujo administrativo soportado.

## API añadida

Todas las rutas exigen una sesión ADM; las escrituras también requieren CSRF.

| Método | Ruta | Entrada o resultado |
|---|---|---|
| POST | `/api/cuentas` | Identificador, contraseña inicial y roles; devuelve 201 con cuenta/roles/versión, sin credenciales. |
| PUT | `/api/cuentas/{id}` | Identificador, activa y versión leída; devuelve la cuenta actualizada. |
| GET | `/api/cuentas/{id}/auditoria?pagina=0` | Historial de la cuenta, 20 registros por página. |

El directorio y las rutas de roles existentes se mantienen. Los errores distinguen datos inválidos (400), falta de sesión (401), permiso/CSRF (403), cuenta inexistente (404), identificador duplicado/edición obsoleta/último ADM (409) y fallo de persistencia (503).

## Verificación realizada

- `RUN_POSTGRES_TESTS=true ./mvnw clean verify`, desde backend: **58 pruebas, cero fallos y cero omisiones**.
- `npm test`, desde frontend: **27 pruebas aprobadas**.
- `npm run build`: compilación de TypeScript y producción correcta.
- Pruebas HTTP reales contra PostgreSQL: alta con hash y login, desactivación de sesión abierta, reactivación con nuevo login, duplicados, altas concurrentes, conflictos entre edición y roles, retirada/desactivación simultánea de administradores, CSRF, permisos revocados y reversión ante fallo de auditoría.
- Pruebas de interfaz: altas, confirmación de contraseña, errores de duplicado, envío de versión, desactivación, conflictos, historial, visibilidad de permisos y análisis Axe de los formularios (contraste excluido en jsdom). Esto no equivale a certificación de accesibilidad completa.
- Las pruebas de administración usan un esquema PostgreSQL aislado y lo eliminan al terminar; no modifican la cuenta local de Bryan.

## Vínculo profesional y simplicidad

El vínculo usa una columna nullable `cuentas.profesional_id`: una cuenta tiene como máximo una ficha. Una clave foránea impide referenciar una ficha inexistente y una restricción única impide compartirla entre cuentas. Una ficha vinculada sigue reservada para esa cuenta aunque la cuenta se desactive; para transferirla se desvincula expresamente.

El caso de uso exige PRO y una ficha activa para vincular, comprueba la versión y registra ficha anterior/nueva junto con autor y fecha. Para retirar PRO primero se debe desvincular; no se borra el vínculo silenciosamente. Un guardado idéntico no genera auditoría artificial. Desactivar una cuenta conserva el vínculo y su historia.

La escritura reutiliza el bloqueo y la transacción de cuentas/roles. Dos cuentas que intentan vincular la misma ficha no pueden conseguirlo simultáneamente. Si falla la auditoría se deshace el vínculo completo. La versión también cambia al vincular/desvincular, por lo que no se sobrescriben ediciones concurrentes.

El contrato público `FichasProfesionales` permite que identidad consulte el módulo administrativo sin acceder directamente a sus tablas. El registro básico es una sola inserción validada por `FichaProfesional`; no se añadió una clase intermedia que solo delegue esa llamada. La ficha registra quién la creó y cuándo. La búsqueda devuelve hasta 50 resultados y permite refinar por nombre o identificación; una ficha ya vinculada se consulta por ID aunque no aparezca en esos 50 resultados.

RF-07 amplió esta ficha básica con edición, estado, versión y servicios habilitados — ver [docs/PROFESIONALES.md](PROFESIONALES.md). Las rutas `/api/profesionales/fichas*` exigen ahora el permiso `PROFESIONALES_GESTIONAR` en lugar de `CUENTAS_GESTIONAR`; el vínculo de esta sección (`PUT /api/cuentas/{id}/profesional`) no cambió y sigue exigiendo `CUENTAS_GESTIONAR`.

### Rutas del vínculo

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/profesionales/fichas?busqueda=texto` | Buscar fichas por nombre/identificación. |
| GET | `/api/profesionales/fichas/{id}` | Consultar una ficha, incluida la actualmente vinculada. |
| POST | `/api/profesionales/fichas` | Registrar identificación, nombre y especialidad/función. |
| PUT | `/api/cuentas/{id}/profesional` | Guardar `{ "profesionalId": null, "version": 4 }`; null desvincula. |

Todas exigen ADM (las tres rutas de `/api/profesionales/fichas` con `PROFESIONALES_GESTIONAR` desde RF-07; el vínculo con `CUENTAS_GESTIONAR`); las escrituras requieren CSRF. Se informa si la ficha no existe/está inactiva, está ocupada, falta PRO o la versión quedó obsoleta. Se probaron alta/consulta, reemplazo/desvinculación, permisos, CSRF, duplicación, concurrencia y reversión ante fallo de auditoría.

### Dónde cambiar cada comportamiento

- **Reglas de cuentas y vínculo:** `GestionarCuentas.java`; cambiar la regla aquí y su prueba de integración. No duplicarla en React.
- **Retirar roles:** `CambiarRoles.java`; aquí se protege el vínculo y al último administrador.
- **Rutas y respuestas:** `GestionCuentasController.java`.
- **SQL, transacción y auditoría de acceso:** `GestionAccesoPostgres.java`.
- **Datos básicos del profesional:** `FichaProfesional.java`, `FichasProfesionalesController.java` y `FichasProfesionalesPostgres.java`.
- **Pantallas:** `FormulariosCuenta.tsx` (alta/estado), `VinculoProfesional.tsx` (ficha/vínculo), `RolesPanel.tsx` (directorio, roles e historial).
- **Llamadas HTTP de la pantalla:** `api/cuentas.ts` y `api/profesionales.ts`; reutilizan el cliente y CSRF existentes.
- **Nuevas columnas o restricciones:** agregar otra migración; no editar V1–V8.

## Correspondencia con RF-02

| Criterio | Implementación |
|---|---|
| CA-1: identificador único y vínculo único | Restricciones de PostgreSQL y validación del caso de uso. |
| CA-2: retirar rol/desactivar afecta sesiones abiertas | Roles consultados en cada solicitud y revocación por versión de acceso. |
| CA-3: conservar un administrador activo | Validación de estado/roles bajo el mismo bloqueo transaccional. |
| CA-4: auditoría de cambios de acceso | Auditoría transaccional de cuentas, roles y vínculo, sin secretos. |

RF-03 y la gestión completa de profesionales (RF-07) siguen siendo funcionalidades independientes. No se implementaron agendas, servicios ni caja. Cuando se implemente la desactivación de fichas, deberá coordinarse con las reglas de vínculos y citas; hoy el registro básico solo permite alta y consulta.
