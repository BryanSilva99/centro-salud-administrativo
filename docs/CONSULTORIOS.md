# Consultorios — RF-09

Actualizado: 11 de octubre de 2026. **RF-09 implementado:** crear, listar, editar, activar/desactivar consultorios y sus servicios compatibles.

## Uso

1. Ingresar con una cuenta ADM y pulsar **Gestionar consultorios**.
2. **Nuevo consultorio**: código, nombre y los servicios compatibles que correspondan (puede quedar sin ninguno).
3. Seleccionar un consultorio de la lista para editarlo: código, nombre, estado y servicios compatibles.
4. Si otra persona guardó el consultorio mientras lo editabas, el guardado se bloquea y se ofrece **Recargar consultorio**.

## Reglas

- **Código:** obligatorio, hasta 20 caracteres, lo escribe ADM (no se genera automáticamente) y es único sin distinguir mayúsculas ("C-100" y "c-100" son el mismo).
- **Nombre:** obligatorio, hasta 150 caracteres.
- **Servicios compatibles:** una relación simple que se reemplaza por completo en cada edición (como los roles de una cuenta). Un consultorio sin servicios compatibles es válido: simplemente no puede recibir reservas, lo cual se aplicará en RF-13.
- **Sin borrado físico:** un consultorio se desactiva (RN-03), nunca se elimina.

## Cómo funciona y por qué

| Capa | Archivos | Responsabilidad |
|---|---|---|
| React | `ConsultoriosPanel.tsx`, `ConsultorioForm.tsx`, `administration/api/consultorios.ts` | Listar, editar, enviar la versión leída y comunicar conflictos. |
| Entrada HTTP | `ConsultoriosController` | Validar la forma de la solicitud, tomar el autor de la sesión y traducir errores. |
| Aplicación | `GestionarConsultorios` | Alta, comprobación de versión y de que cada servicio compatible exista; omite guardados idénticos. |
| Dominio | `Consultorio` | Código, nombre y sus invariantes. |
| Persistencia | `ConsultoriosPostgres`, migración V11 | Escritura condicionada por versión y auditoría en la misma transacción; unicidad del código en PostgreSQL. |
| Autorización | `PermisosCuenta`, `SecurityConfiguration` | `CONSULTORIOS_GESTIONAR` solo para ADM; todas las rutas lo exigen. |

**Concurrencia (RNF-03).** La edición ejecuta `UPDATE ... SET version = version + 1 WHERE id = ? AND version = ?`, igual que `servicios`. Las altas simultáneas con el mismo código las resuelve el índice único `consultorios_codigo_unique`.

**Auditoría.** `auditoria_consultorios` guarda el actor, la fecha, el tipo y una foto JSON completa (código, nombre, estado, versión y servicios compatibles), antes y después del cambio. El catálogo no contiene datos personales, así que la foto completa es aceptable (a diferencia de pacientes). Si falla la auditoría se revierte todo el cambio. Un guardado sin cambios no incrementa la versión ni audita.

**Lectura solo para ADM (pendiente).** Hoy `GET /api/consultorios` exige `CONSULTORIOS_GESTIONAR`, igual que las escrituras. Cuando se implemente RF-13 (programar citas), recepción necesitará su propia lectura de este catálogo para elegir un consultorio compatible y activo; eso no está implementado todavía.

## API

Todas las rutas exigen `CONSULTORIOS_GESTIONAR` (ADM); las escrituras requieren CSRF.

| Método | Ruta | Entrada o resultado |
|---|---|---|
| GET | `/api/consultorios` | Todos los consultorios, activos e inactivos, por código, sin paginar. |
| GET | `/api/consultorios/{id}` | Un consultorio con sus servicios compatibles. 404 si no existe. |
| POST | `/api/consultorios` | `codigo`, `nombre` y `serviciosCompatibles[]` (IDs de servicio). Devuelve 201 con versión 0. |
| PUT | `/api/consultorios/{id}` | Igual que el alta, más `activo` y la `version` leída. |

Errores: 400 (datos inválidos o `SERVICIO_INVALIDO`), 401 (sin sesión), 403 (sin permiso o CSRF), 404 (`NO_EXISTE`), 409 (`CONFLICTO` o `CODIGO_DUPLICADO`), 503 (fallo de persistencia).

## Verificación realizada

- `RUN_POSTGRES_TESTS=true ./mvnw clean verify`, desde backend: **115 pruebas, cero fallos y cero omisiones**.
- `npm test`, desde frontend: **48 pruebas aprobadas**. `npm run build` correcto.
- PostgreSQL (`ConsultoriosHttpTest`, esquema aislado):
  - alta, listado y consulta con auditoría;
  - la edición incrementa la versión y audita el estado anterior y el nuevo;
  - una versión obsoleta recibe 409;
  - código duplicado (sin distinguir mayúsculas) y servicio inexistente se rechazan;
  - un guardado idéntico no audita;
  - **10 altas concurrentes con el mismo código → 1 creado, 9 `CODIGO_DUPLICADO`**;
  - **10 ediciones concurrentes con la misma versión → 1 aplicada, 9 `CONFLICTO`**;
  - un fallo de auditoría revierte el alta y la edición;
  - REC, CAJ, PRO y cuenta sin roles reciben 403; ADM+REC puede gestionar;
  - sin sesión 401, sin CSRF 403, rol retirado 403, cuenta desactivada 401;
  - datos inválidos reciben 400.
- Interfaz: alta con servicios seleccionados, edición con versión preservando la selección, conflicto con recarga, duplicado, listado, visibilidad por permiso y análisis Axe (contraste excluido en jsdom).

## Pendiente (depende de requisitos fuera de alcance)

- **RF-09/CA-2:** que un consultorio incompatible o inhabilitado no reciba nuevas reservas — depende de que exista la reserva de citas (RF-13).
- **RF-09/CA-3:** mostrar las citas futuras afectadas antes de inhabilitar un consultorio (RN-08) — depende de que existan citas.
- **Lectura de recepción:** un contrato de solo lectura para que REC consulte consultorios activos y compatibles al programar una cita (RF-12/RF-13).

## Dónde cambiar cada comportamiento

- **Reglas de código, nombre y servicios compatibles:** `Consultorio.java`. Las restricciones `CHECK` de la tabla deben cambiar en una nueva migración.
- **Versión o guardados idénticos:** `GestionarConsultorios.java`.
- **SQL, concurrencia y auditoría:** `ConsultoriosPostgres.java`.
- **Quién gestiona consultorios:** `PermisosCuenta.java` y las rutas en `SecurityConfiguration.java`.
- **Nuevas columnas o restricciones:** agregar otra migración; no editar V11.
