# Pacientes — RF-04 y RF-05

Actualizado: 11 de octubre de 2026. **RF-04 y RF-05 implementados:** alta, búsqueda y actualización de pacientes por recepción, con código interno, documento único, versión de edición y auditoría. CA-3 de RF-05 (lectura mínima de caja y de profesionales) sigue pendiente; ver sección de pendientes.

## Uso

1. Ingresar con una cuenta que tenga el rol REC.
2. **Registrar paciente**: elegir el tipo de documento, escribir el número, los nombres, los apellidos y la fecha de nacimiento. El teléfono y el correo son opcionales. Al guardar, la pantalla muestra el código asignado (por ejemplo, `P00000042`).
3. **Buscar pacientes**: escribir documento, nombre o código y pulsar **Buscar**. La lista es paginada; seleccionar un resultado abre su edición.
4. Al editar, el código interno no se puede cambiar. Si otra persona guardó el paciente mientras lo editabas, el guardado se bloquea y se ofrece **Recargar paciente**.

Si el documento ya está registrado (en el alta o al cambiarlo en una edición), se rechaza la operación y se indica que ya existe un registro con ese documento. Si la red falla durante un alta, no se reintenta automáticamente: pudo haberse guardado. Antes de RF-05 había que confirmar con el equipo; ahora se puede usar **Buscar pacientes** para comprobarlo. La restricción única de la base impide igualmente un duplicado.

## Reglas (RN-04, RN-05)

- **Documento:** DNI de 8 dígitos o carné de extranjería (CE) de 9 letras o números. El CE se guarda en mayúsculas, así que `ab1234567` y `AB1234567` son el mismo documento. Validar el formato no confirma la identidad.
- **Unicidad:** el par tipo + número es único, tanto al crear como al editar. Dos pacientes con el mismo nombre y documentos distintos son válidos. El mismo número con distinto tipo también.
- **Datos obligatorios:** nombres y apellidos (hasta 100 caracteres) y fecha de nacimiento entre 1900-01-01 y hoy. "Hoy" es la fecha de Lima, no la de UTC.
- **Contacto:** el teléfono y el correo solo se validan por forma, no se verifican.
- **Edición (RF-05/CA-2):** el código interno nunca cambia. Cambiar el documento vuelve a verificar la unicidad; un documento ya usado por otro paciente se rechaza igual que en el alta.
- **Conflicto de edición (RF-05/CA-4):** cada paciente tiene una versión. Guardar con una versión obsoleta se rechaza con 409 en vez de sobrescribir en silencio el cambio de otra persona.

## Cómo funciona y por qué

| Capa | Archivos | Responsabilidad |
|---|---|---|
| React | `RegistrarPacienteForm.tsx` (alta), `BuscarPacientesPanel.tsx` + `EditarPacienteForm.tsx` (búsqueda y edición), `administration/api/pacientes.ts` | Capturar los datos, evitar el doble envío, enviar la versión leída y comunicar conflictos. |
| Entrada HTTP | `PacientesController` | Validar la forma de la solicitud, tomar el autor de la sesión y traducir errores. La búsqueda es de solo lectura y usa el puerto directamente, sin caso de uso propio. |
| Aplicación | `RegistrarPaciente` (alta), `ActualizarPaciente` (edición) | Validar la fecha con el reloj del centro; la edición además comprueba la versión y conserva el código. Ninguno depende de Spring ni de SQL. |
| Dominio | `Paciente`, `TipoDocumento` | Formatos de documento, campos obligatorios, normalización y versión. |
| Persistencia | `PacientesPostgres`, migraciones V9 y V14 | Alta, búsqueda paginada y actualización condicionada por versión, con auditoría en la misma transacción. PostgreSQL asigna el código y garantiza la unicidad del documento, también al editar. |
| Autorización | `PermisosCuenta`, `SecurityConfiguration` | `PACIENTES_GESTIONAR` solo para REC; las tres rutas (`GET`, `POST`, `PUT`) lo exigen. |

**Concurrencia (RNF-03).** La restricción `pacientes_documento_unique` decide entre altas o cambios de documento simultáneos: uno se guarda y los demás reciben 409 `DOCUMENTO_DUPLICADO`. La edición ejecuta `UPDATE ... SET version = version + 1 WHERE id = ? AND version = ?`, igual que `servicios`/`consultorios`/`profesionales`: entre dos ediciones concurrentes con la misma versión, solo una se aplica y las demás reciben 409 `CONFLICTO`. No hay una consulta previa ni un bloqueo adicional, porque la base ya garantiza ambos resultados.

**Código interno.** Lo genera la secuencia `pacientes_codigo_seq` con el formato `P` + 8 dígitos, solo al crear. Si un alta se revierte, su número no se reutiliza y queda un hueco. La edición nunca lo toca.

**Autorización.** Se exige en la ruta. `ValidarSesionFilter` recalcula los permisos en cada solicitud a partir de los roles vigentes y del estado de la cuenta, así que retirar REC o desactivar la cuenta se aplica a la siguiente solicitud. ADM no recibe este permiso sin el rol REC (matriz de acceso, sección 2.2).

**Auditoría.** `auditoria_pacientes` registra el actor, el paciente, el tipo (`CREACION` o, desde RF-05, `ACTUALIZACION`) y la fecha. Sigue sin copiar datos personales (RN-17), a diferencia de servicios/consultorios/profesionales/turnos, que no son datos personales y sí llevan una foto JSON completa. Si falla la auditoría, el alta o la edición completa se revierte (RF-28/CA-3). Un guardado idéntico no incrementa la versión ni audita. La consulta de esta auditoría pertenece a RF-28.

## API

Todas las rutas exigen `PACIENTES_GESTIONAR` (REC); las escrituras requieren CSRF.

| Método | Ruta | Entrada o resultado |
|---|---|---|
| GET | `/api/pacientes?busqueda=texto&pagina=0` | Busca por código, documento o nombre; 20 elementos por página. |
| POST | `/api/pacientes` | `tipoDocumento`, `numeroDocumento`, `nombres`, `apellidos`, `fechaNacimiento` (`AAAA-MM-DD`), `telefono` y `correo` opcionales. Devuelve 201 con el paciente y su `codigo`. |
| PUT | `/api/pacientes/{id}` | Igual que el alta, más la `version` leída. El código interno no se envía: lo conserva la edición. |

Errores: 400 (datos inválidos), 401 (sin sesión), 403 (sin permiso o CSRF), 404 (`NO_EXISTE`), 409 (`DOCUMENTO_DUPLICADO` o `CONFLICTO`), 503 (fallo de persistencia).

## Verificación realizada

- `RUN_POSTGRES_TESTS=true ./mvnw clean verify`, desde backend: **162 pruebas, cero fallos y cero omisiones**.
- `npm test`, desde frontend: **67 pruebas aprobadas**. `npm run build` correcto.
- PostgreSQL (`PacientesHttpTest`, esquema aislado):
  - alta con código y auditoría sin datos personales; homónimos aceptados; duplicado rechazado, también con CE en minúsculas;
  - **10 altas concurrentes del mismo documento producen un solo paciente**; 10 altas concurrentes con documentos distintos producen 10 códigos distintos;
  - búsqueda por documento, nombre y código, con paginación;
  - la edición conserva el código y vuelve a verificar la unicidad al cambiar el documento (CA-2);
  - una versión obsoleta recibe 409 `CONFLICTO`; un guardado idéntico no audita;
  - **10 ediciones concurrentes con la misma versión → 1 aplicada, 9 `CONFLICTO`**;
  - **10 ediciones concurrentes que cambian el documento al mismo valor nuevo → 1 aplicada, 9 `DOCUMENTO_DUPLICADO`**;
  - reversión ante fallo de auditoría (alta y edición);
  - ADM, CAJ, PRO y cuenta sin roles reciben 403 en las tres rutas; ADM+REC puede gestionar;
  - sin sesión 401, sin CSRF 403, rol retirado 403, cuenta desactivada 401;
  - datos inválidos 400.
- Interfaz: alta, búsqueda paginada, edición con versión, conflicto con recarga, duplicado, fallo de red, doble envío, sesión vencida, visibilidad por permiso y análisis Axe (contraste excluido en jsdom). No equivale a certificación de accesibilidad.

## Pendiente (depende de requisitos fuera de alcance)

- **RF-05/CA-3:** lectura mínima para caja ("para cobro") y para profesionales ("de sus citas"). Ambas dependen de módulos fuera de este alcance: caja y citas (RF-19+, RF-13+). Hoy solo REC tiene acceso a `/api/pacientes`; no se agregó un permiso de solo lectura especulativo.

## Dónde cambiar cada comportamiento

- **Formatos o campos obligatorios:** `Paciente.java` y `TipoDocumento.java`. Las restricciones `CHECK` de la tabla deben cambiar en una nueva migración.
- **Fecha válida de nacimiento:** `Paciente.validarNacimiento`, usada por `RegistrarPaciente` y `ActualizarPaciente`.
- **Reglas de edición (versión, código conservado):** `ActualizarPaciente.java`.
- **SQL, búsqueda, concurrencia y auditoría:** `PacientesPostgres.java`.
- **Quién puede gestionar pacientes:** `PermisosCuenta.java` y las rutas en `SecurityConfiguration.java`.
- **Nuevas columnas o restricciones:** agregar otra migración; no editar V9 ni V14.
