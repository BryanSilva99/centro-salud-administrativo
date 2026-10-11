# Registro de pacientes — RF-04

Actualizado: 10 de octubre de 2026. **RF-04 implementado:** alta de pacientes por recepción con código interno, documento único y auditoría. La búsqueda, la consulta y la edición (RF-05) siguen pendientes.

## Uso

1. Ingresar con una cuenta que tenga el rol REC y pulsar **Registrar paciente**.
2. Elegir el tipo de documento, escribir el número, los nombres, los apellidos y la fecha de nacimiento. El teléfono y el correo son opcionales.
3. Al guardar, la pantalla muestra el código asignado (por ejemplo, `P00000042`).

Si el documento ya está registrado, se rechaza el alta y se indica usar el registro existente. Si la red falla durante el envío, no se reintenta automáticamente: el alta pudo haberse guardado. Hasta que exista la búsqueda de RF-05, se debe confirmar con el equipo antes de repetirla. La restricción única impide igualmente un duplicado.

## Reglas (RN-04)

- **Documento:** DNI de 8 dígitos o carné de extranjería (CE) de 9 letras o números. El CE se guarda en mayúsculas, así que `ab1234567` y `AB1234567` son el mismo documento. Validar el formato no confirma la identidad.
- **Unicidad:** el par tipo + número es único. Dos pacientes con el mismo nombre y documentos distintos son válidos. El mismo número con distinto tipo también.
- **Datos obligatorios:** nombres y apellidos (hasta 100 caracteres) y fecha de nacimiento entre 1900-01-01 y hoy. "Hoy" es la fecha de Lima, no la de UTC.
- **Contacto:** el teléfono y el correo solo se validan por forma, no se verifican.

## Cómo funciona y por qué

| Capa | Archivos | Responsabilidad |
|---|---|---|
| React | `RegistrarPacienteForm.tsx`, `administration/api/pacientes.ts` | Capturar los datos, evitar el doble envío y mostrar el código o el error. Usa el cliente HTTP y el CSRF existentes. |
| Entrada HTTP | `PacientesController` | Validar la forma de la solicitud, tomar el autor de la sesión y traducir errores. Las respuestas no repiten el documento. |
| Aplicación | `RegistrarPaciente` | Validar la fecha con el reloj del centro, construir el paciente y registrarlo. No depende de Spring ni de SQL. |
| Dominio | `Paciente`, `TipoDocumento` | Formatos de documento, campos obligatorios y normalización. |
| Persistencia | `PacientesPostgres`, migración V9 | Insertar el paciente y su auditoría en una transacción. PostgreSQL asigna el código y garantiza la unicidad. |
| Autorización | `PermisosCuenta`, `SecurityConfiguration` | `PACIENTES_GESTIONAR` solo para REC; la ruta lo exige. |

**Concurrencia.** La restricción `pacientes_documento_unique` decide entre altas simultáneas del mismo documento: una se guarda y las demás reciben 409. No hay una consulta previa ni un bloqueo adicional, porque la base ya garantiza el resultado.

**Código interno.** Lo genera la secuencia `pacientes_codigo_seq` con el formato `P` + 8 dígitos. Si un alta se revierte, su número no se reutiliza y queda un hueco. Es un identificador, no un contador de pacientes.

**Autorización.** Se exige en la ruta. `ValidarSesionFilter` recalcula los permisos en cada solicitud a partir de los roles vigentes y del estado de la cuenta, así que retirar REC o desactivar la cuenta se aplica a la siguiente solicitud. ADM no recibe este permiso sin el rol REC (matriz de acceso, sección 2.2).

**Auditoría.** `auditoria_pacientes` registra el actor, el paciente, el tipo (`CREACION`) y la fecha. No copia datos personales (RN-17). Si falla la auditoría, el alta completa se revierte (RF-28/CA-3). La consulta de esta auditoría pertenece a RF-28.

## API

| Método | Ruta | Entrada o resultado |
|---|---|---|
| POST | `/api/pacientes` | `tipoDocumento`, `numeroDocumento`, `nombres`, `apellidos`, `fechaNacimiento` (`AAAA-MM-DD`), `telefono` y `correo` opcionales. Devuelve 201 con el paciente y su `codigo`. |

Requiere una sesión con `PACIENTES_GESTIONAR` y CSRF. Errores: 400 (datos inválidos), 401 (sin sesión), 403 (sin permiso o CSRF), 409 `DOCUMENTO_DUPLICADO` y 503 (fallo de persistencia).

## Verificación realizada

- `RUN_POSTGRES_TESTS=true ./mvnw clean verify`, desde backend: **76 pruebas, cero fallos**.
- `npm test`, desde frontend: **34 pruebas aprobadas**. `npm run build` correcto.
- PostgreSQL (`PacientesHttpTest`, esquema aislado):
  - alta con código y auditoría sin datos personales;
  - homónimos aceptados; duplicado rechazado, también con CE en minúsculas;
  - 10 altas concurrentes del mismo documento producen un solo paciente;
  - 10 altas concurrentes con documentos distintos producen 10 códigos distintos;
  - reversión ante fallo de auditoría;
  - ADM, CAJ, PRO y cuenta sin roles reciben 403; ADM+REC registra;
  - sin sesión 401, sin CSRF 403, rol retirado 403, cuenta desactivada 401;
  - datos inválidos 400.
- Interfaz: envío correcto, duplicado, fallo de red, doble envío, sesión vencida, visibilidad por permiso y análisis Axe (contraste excluido en jsdom). No equivale a certificación de accesibilidad.

## Dónde cambiar cada comportamiento

- **Formatos o campos obligatorios:** `Paciente.java` y `TipoDocumento.java`. La restricción `CHECK` de la tabla debe cambiar en una nueva migración.
- **Fecha válida de nacimiento:** `Paciente.validarNacimiento` y la zona en `RegistrarPaciente`.
- **SQL y auditoría:** `PacientesPostgres.java`.
- **Quién puede registrar:** `PermisosCuenta.java` y la ruta en `SecurityConfiguration.java`.
- **Nuevas columnas (por ejemplo, la versión de edición de RF-05):** agregar otra migración; no editar V9.
