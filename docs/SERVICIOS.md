# Servicios y condiciones económicas — RF-08

Actualizado: 10 de octubre de 2026. **RF-08 implementado parcialmente:** crear, listar, editar y activar/desactivar servicios con nombre, duración, estado y modalidades económicas con o sin cargo. Los profesionales habilitados (RF-07) y los consultorios compatibles (RF-09) se agregarán con esos requisitos.

## Pendiente para la reserva de citas (RN-12, RF-08/CA-3)

> **Al reservar una cita (RF-13), la duración del servicio y la tarifa de la modalidad elegida deben copiarse a la cita.** La cita no debe leer después estos valores del catálogo.

Este módulo permite cambiar la duración y las tarifas en cualquier momento, y no modifica nada fuera de su catálogo. Que las citas y los cargos ya registrados conserven sus condiciones (RF-08/CA-3) depende de que RF-13 guarde su propia copia de:

- la duración en minutos;
- el ID de la modalidad, como referencia;
- el importe y si tiene cargo.

RF-19 debe calcular el cargo desde esa copia, no desde `servicio_modalidades`. Ejemplo de RN-12: una cita reservada a S/40.00 conserva ese importe aunque la tarifa pase después a S/45.00.

## Uso

1. Ingresar con una cuenta ADM y pulsar **Gestionar servicios**.
2. **Nuevo servicio**: nombre, duración y al menos una modalidad. El servicio se crea activo.
3. Seleccionar un servicio de la lista para editarlo: nombre, duración, estado, tarifas y modalidades.
4. **Agregar modalidad** añade una fila. Una modalidad todavía no guardada puede quitarse; una guardada solo puede desactivarse.
5. Si otra persona guardó el servicio mientras lo editabas, el guardado se bloquea y se ofrece **Recargar servicio**.

## Reglas

- **Nombre del servicio:** obligatorio, hasta 100 caracteres y único sin distinguir mayúsculas ("Consulta" y "CONSULTA" son el mismo).
- **Duración:** entre 1 y 480 minutos. El tope de 8 horas atrapa errores de tipeo.
- **Modalidades:**
  - Cada servicio tiene al menos una.
  - Los nombres no se repiten dentro del servicio (hasta 60 caracteres).
  - Una modalidad **con cargo** exige tarifa mayor que S/ 0.00. Una modalidad **sin cargo** tiene tarifa exactamente S/ 0.00 (coherente con RF-19/CA-3).
- **Tarifa:** en soles, no negativa, hasta S/ 99 999 999.99. Admite como máximo 2 decimales: `40.555` se rechaza, no se redondea.
- **Servicio activo:** requiere al menos una modalidad activa; si no, una reserva no tendría importe aplicable (CA-2).
- **Sin borrado físico:** los servicios y las modalidades se desactivan (RN-03). La edición debe incluir todas las modalidades guardadas; omitir una se rechaza para que nunca se pierda en silencio.

## Cómo funciona y por qué

| Capa | Archivos | Responsabilidad |
|---|---|---|
| React | `ServiciosPanel.tsx`, `ServicioForm.tsx`, `administration/api/servicios.ts` | Listar, editar, enviar la versión leída y comunicar conflictos. La tarifa se envía como texto (`"40.50"`) para que llegue exacta. |
| Entrada HTTP | `ServiciosController` | Validar la forma de la solicitud, tomar el autor de la sesión y traducir errores. Listar y consultar usan el puerto directamente. |
| Aplicación | `GestionarServicios` | Alta, comprobación de versión y de identificadores de modalidad; omite guardados idénticos. |
| Dominio | `Servicio`, `Modalidad` | Duración, tarifas, cargo y modalidad activa. |
| Persistencia | `ServiciosPostgres`, migración V10 | Escritura condicionada por versión y auditoría en la misma transacción; unicidad del nombre en PostgreSQL. |
| Autorización | `PermisosCuenta`, `SecurityConfiguration` | `SERVICIOS_GESTIONAR` solo para ADM; todas las rutas lo exigen. |

**Concurrencia (RNF-03).** La edición ejecuta `UPDATE ... SET version = version + 1 WHERE id = ? AND version = ?`. Entre ediciones simultáneas con la misma versión, solo una modifica la fila; las demás reciben `CONFLICTO` y no escriben nada. La versión cubre también los cambios de modalidades, porque toda edición pasa por ese `UPDATE`. Las altas simultáneas con el mismo nombre las resuelve el índice único `servicios_nombre_unique`.

**Auditoría.** `auditoria_servicios` guarda el actor, la fecha, el tipo y una foto JSON completa del servicio y sus modalidades, antes y después del cambio. Se incluye el historial de tarifas que exige RN-17. El catálogo no contiene datos personales. Si falla la auditoría se revierte todo el cambio. Un guardado sin cambios no incrementa la versión ni audita. La consulta de esta auditoría pertenece a RF-28.

## API

Todas las rutas exigen `SERVICIOS_GESTIONAR` (ADM); las escrituras requieren CSRF.

| Método | Ruta | Entrada o resultado |
|---|---|---|
| GET | `/api/servicios` | Todos los servicios, activos e inactivos, por nombre, sin paginar. |
| GET | `/api/servicios/{id}` | Un servicio con sus modalidades. 404 si no existe. |
| POST | `/api/servicios` | `nombre`, `duracionMinutos` y `modalidades[]` (`nombre`, `conCargo`, `tarifa`, `activa`). Devuelve 201 con versión 0. |
| PUT | `/api/servicios/{id}` | Igual que el alta, más `activo` y la `version` leída. Cada modalidad existente lleva su `id`; las nuevas, `id: null`. |

Errores:

- **400:** datos inválidos, o `MODALIDAD_INVALIDA` (una modalidad omitida, repetida o de otro servicio).
- **401:** sin sesión.
- **403:** sin permiso o CSRF.
- **404:** `NO_EXISTE`.
- **409:** `CONFLICTO` (versión obsoleta) o `NOMBRE_DUPLICADO`.
- **503:** fallo de persistencia.

## Verificación realizada

- `RUN_POSTGRES_TESTS=true ./mvnw clean verify`, desde backend: **97 pruebas, cero fallos y cero omisiones**.
- `npm test`, desde frontend: **41 pruebas aprobadas**. `npm run build` correcto.
- PostgreSQL (`ServiciosHttpTest`, esquema aislado):
  - alta, listado y consulta con auditoría;
  - la edición incrementa la versión y audita la tarifa anterior y la nueva;
  - una versión obsoleta recibe 409;
  - desactivar conserva las filas;
  - un guardado idéntico no audita;
  - quitar una modalidad o repetir el nombre (sin distinguir mayúsculas) se rechaza;
  - 10 ediciones concurrentes con la misma versión: 1 se aplica y 9 reciben conflicto;
  - 10 altas concurrentes con el mismo nombre crean 1 servicio;
  - un fallo de auditoría revierte el alta y la edición;
  - tarifas y duraciones inválidas reciben 400;
  - REC, CAJ, PRO y cuenta sin roles reciben 403; ADM+REC puede gestionar;
  - sin sesión 401, sin CSRF 403, rol retirado 403, cuenta desactivada 401.
- Interfaz: alta con tarifa exacta, modalidad sin cargo, edición con versión, desactivación de modalidades, conflicto con recarga, duplicado, listado, visibilidad por permiso y análisis Axe (contraste excluido en jsdom).

## Limitaciones

- Intercambiar los nombres de dos modalidades en una misma edición viola momentáneamente el índice único y responde 503. Se puede hacer en dos ediciones.
- La comparación sin mayúsculas usa `lower()` de PostgreSQL y `toLowerCase(Locale.ROOT)` de Java. Pueden diferir en caracteres poco comunes; la base es la garantía final.
- Todavía no existe un contrato de lectura para RF-13. Se agregará en `application/ports` cuando se implemente la reserva, con REC como lector.

## Dónde cambiar cada comportamiento

- **Reglas de duración, tarifa y modalidades:** `Servicio.java` y `Modalidad.java`. Las restricciones `CHECK` de la tabla deben cambiar en una nueva migración.
- **Versión, modalidades omitidas o guardados idénticos:** `GestionarServicios.java`.
- **SQL, concurrencia y auditoría:** `ServiciosPostgres.java`.
- **Quién gestiona servicios:** `PermisosCuenta.java` y las rutas en `SecurityConfiguration.java`.
- **Profesionales y consultorios habilitados (RF-07, RF-09):** nuevas tablas de relación en otra migración; no editar V10.
