# Disponibilidad — RF-10

Actualizado: 11 de octubre de 2026. **RF-10 implementado parcialmente:** definir turnos (intervalos con descansos) para profesionales y consultorios, con sus restricciones de validez y de superposición. La consulta combinada de disponibilidad (CA-1) y el impacto en citas existentes (CA-3) quedan pendientes; ver sección de pendientes.

## Uso

1. Ingresar con una cuenta ADM y pulsar **Gestionar disponibilidad**.
2. Elegir el tipo de recurso (profesional o consultorio) y el recurso concreto.
3. Se muestran sus turnos existentes, con la opción de **Desactivar** los activos.
4. **Nuevo turno**: fecha, hora de inicio, hora de fin y tipo (disponible o descanso).

Un turno no se edita: para cambiar un horario se desactiva el turno anterior y se crea uno nuevo.

## Reglas

- **Intervalo:** la hora de fin debe ser posterior a la de inicio. No se admiten turnos que cruzan la medianoche.
- **Fecha y horas:** son valores civiles de América/Lima (RN-11), tal como los escribe ADM. No son un instante UTC, a diferencia de la fecha de la auditoría.
- **Tipo:** `DISPONIBLE` o `DESCANSO`. No existe un tipo de bloqueo aquí: eso corresponde a RF-11 (fuera de alcance). Un horario sin turno `DISPONIBLE` ya es "no disponible" por defecto.
- **Superposición (RN-07):** dentro de un mismo recurso y fecha, ningún par de turnos activos puede superponerse, sin importar su tipo. Dos turnos son adyacentes (uno termina cuando empieza el otro) y no se consideran superpuestos — por ejemplo, 08:00–12:00 `DISPONIBLE`, 12:00–13:00 `DESCANSO` y 13:00–17:00 `DISPONIBLE` son tres turnos válidos y disjuntos.
- **Sin borrado físico:** un turno se desactiva, nunca se elimina. Desactivar un turno ya inactivo no tiene efecto ni se vuelve a auditar.

## Cómo funciona y por qué

| Capa | Archivos | Responsabilidad |
|---|---|---|
| React | `DisponibilidadPanel.tsx`, `TurnoForm.tsx`, `administration/api/disponibilidad.ts` | Elegir recurso y fecha, listar turnos, crear y desactivar. |
| Entrada HTTP | `DisponibilidadController` | Traducir el segmento `{tipo}` de la ruta (`profesionales`/`consultorios`) a `TipoRecurso` y las respuestas de error. |
| Aplicación | `GestionarDisponibilidad` | Comprueba que el recurso exista antes de escribir. |
| Dominio | `Turno`, `TipoTurno`, `TipoRecurso` | El intervalo y su validez; `seSuperponeCon` expresa la regla de cruce en rango semiabierto `[inicio,fin)`. |
| Persistencia | `DisponibilidadPostgres`, migración V13 | Bloqueo de la fila del recurso, comprobación de superposición e inserción, todo en una transacción. |
| Autorización | `PermisosCuenta`, `SecurityConfiguration` | `DISPONIBILIDAD_GESTIONAR` solo para ADM; todas las rutas lo exigen. |

**Por qué un solo adaptador para dos tablas.** `turnos_profesional` y `turnos_consultorio` son dos tablas (esquema duplicado a propósito, para que cada una tenga su propia clave foránea sin una columna doble nulable), pero `DisponibilidadPostgres` es una sola clase que elige el nombre de tabla y de columna según `TipoRecurso`. No se duplica el código Java, solo el esquema.

**Concurrencia (RNF-03), sin `EXCLUDE`/GiST.** La cuenta de la aplicación no es superusuaria y no puede ejecutar `CREATE EXTENSION`; además, las pruebas de integración usan esquemas aislados que se eliminan al terminar, y una extensión es global a la base, no al esquema. Por eso la superposición se resuelve en la aplicación: dentro de la misma transacción del alta se bloquea la fila del recurso (`SELECT ... FOR UPDATE` sobre `profesionales` o `consultorios`, igual que el mutex de `control_administracion` en identidad, pero reutilizando la propia fila del recurso en vez de una tabla aparte), se comprueba si el nuevo intervalo se cruza con otro turno activo del mismo recurso y fecha, y solo entonces se inserta. Dos altas concurrentes del mismo recurso se serializan en ese bloqueo: la segunda espera a que la primera confirme o revierta antes de repetir su propia comprobación.

**Auditoría.** `auditoria_turnos` registra el actor, el tipo de recurso, el turno y el tipo de evento (`CREACION` o `DESACTIVACION`). Es un registro mínimo por evento, como `auditoria_pacientes`, porque un turno no se edita: no hace falta una foto antes/después.

**Lectura solo para ADM (pendiente).** Hoy `GET /api/disponibilidad/{tipo}/{recursoId}` exige `DISPONIBILIDAD_GESTIONAR`. Cuando se implemente RF-13 (programar citas), recepción necesitará su propia lectura de disponibilidad; eso no está implementado todavía.

## API

Todas las rutas exigen `DISPONIBILIDAD_GESTIONAR` (ADM); las escrituras requieren CSRF. `{tipo}` es `profesionales` o `consultorios`.

| Método | Ruta | Entrada o resultado |
|---|---|---|
| GET | `/api/disponibilidad/{tipo}/{recursoId}` | Todos los turnos del recurso, activos e inactivos, ordenados por fecha y hora. |
| POST | `/api/disponibilidad/{tipo}/{recursoId}` | `fecha`, `horaInicio`, `horaFin`, `tipo` (`DISPONIBLE`/`DESCANSO`). Devuelve 201. |
| PUT | `/api/disponibilidad/{tipo}/{recursoId}/{turnoId}` | `{"activo": false}`; desactiva el turno. Idempotente. |

Errores: 400 (intervalo inválido o `RECURSO_INVALIDO`), 401 (sin sesión), 403 (sin permiso o CSRF), 404 (`NO_EXISTE`), 409 (`SUPERPUESTO`), 503 (fallo de persistencia).

## Verificación realizada

- `RUN_POSTGRES_TESTS=true ./mvnw clean verify`, desde backend: **150 pruebas, cero fallos y cero omisiones**.
- `npm test`, desde frontend: **61 pruebas aprobadas**. `npm run build` correcto.
- PostgreSQL (`DisponibilidadHttpTest`, esquema aislado):
  - alta con auditoría; listado ordenado;
  - turnos adyacentes se aceptan; un turno que se cruza con otro existente recibe 409 `SUPERPUESTO`;
  - intervalo inválido y recurso inexistente reciben 400;
  - desactivar es idempotente y no audita dos veces; un turno inactivo libera el horario para uno nuevo;
  - desactivar un turno inexistente recibe 404;
  - **10 altas concurrentes con intervalos que se superponen entre sí, del mismo recurso y fecha → exactamente 1 creado y 9 `SUPERPUESTO`** (RNF-03);
  - 10 altas concurrentes con intervalos disjuntos del mismo recurso → las 10 se crean;
  - REC, CAJ, PRO y cuenta sin roles reciben 403; ADM+REC puede gestionar;
  - sin sesión 401, sin CSRF 403, rol retirado 403, cuenta desactivada 401.
- Interfaz: alta de turno, error de superposición, listado con desactivación, cambio de tipo de recurso, visibilidad por permiso y análisis Axe (contraste excluido en jsdom).

## Pendiente (depende de requisitos fuera de alcance)

- **RF-10/CA-1 completo:** "la consulta de disponibilidad combina turno, habilitación, duración del servicio, bloqueos y reservas existentes" — aquí solo se implementa el turno crudo; el cálculo combinado depende de servicios habilitados por cita (RF-13), de la duración del servicio elegido y de las reservas mismas.
- **RF-10/CA-3:** que modificar un turno no deje citas existentes fuera de disponibilidad sin el tratamiento de RN-08 — depende de que existan citas (RF-13) y de RF-11 (ausencias y bloqueos con impacto).
- **Lectura de recepción:** un contrato de solo lectura para que REC consulte la disponibilidad al programar una cita (RF-12/RF-13).

## Dónde cambiar cada comportamiento

- **Reglas del intervalo y de superposición:** `Turno.java`.
- **Validación del recurso:** `GestionarDisponibilidad.java`.
- **Bloqueo, comprobación de superposición, SQL y auditoría:** `DisponibilidadPostgres.java`.
- **Quién gestiona disponibilidad:** `PermisosCuenta.java` y las rutas en `SecurityConfiguration.java`.
- **Nuevas columnas o restricciones:** agregar otra migración; no editar V13.
