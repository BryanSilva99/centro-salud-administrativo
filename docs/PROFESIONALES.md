# Profesionales — RF-07

Actualizado: 11 de octubre de 2026. **RF-07 implementado:** crear, listar, editar, activar/desactivar profesionales y sus servicios habilitados, extendiendo la ficha básica que dejó RF-02.

## Uso

1. Ingresar con una cuenta ADM y pulsar **Gestionar profesionales**.
2. **Nueva ficha profesional**: identificación, nombre, especialidad/función y los servicios que el profesional pueda atender (puede quedar sin ninguno).
3. Seleccionar una ficha de la lista para editarla: identificación, nombre, función, estado y servicios habilitados.
4. Si otra persona guardó la ficha mientras la editabas, el guardado se bloquea y se ofrece **Recargar ficha**.

El vínculo entre una cuenta con rol PRO y su ficha sigue siendo el de RF-02 (ver [docs/GESTION_CUENTAS.md](GESTION_CUENTAS.md)); esta pantalla no lo modifica.

## Reglas

- **Identificación, nombre y función:** obligatorios, con los mismos límites de RF-02. La identificación es única (restricción de PostgreSQL, sin distinguir mayúsculas la verificación de duplicados ya existente).
- **Servicios habilitados:** una relación simple que se reemplaza por completo en cada edición (como los roles de una cuenta). Una ficha sin servicios habilitados es válida: simplemente no puede recibir reservas, lo cual se aplicará en RF-13 (CA-1).
- **Sin borrado físico:** una ficha se desactiva (RN-03), nunca se elimina. El vínculo con una cuenta se conserva aunque la ficha se desactive.

## Cómo funciona y por qué

| Capa | Archivos | Responsabilidad |
|---|---|---|
| React | `ProfesionalesPanel.tsx`, `ProfesionalForm.tsx`, `administration/api/profesionales.ts` | Listar, editar, enviar la versión leída y comunicar conflictos. |
| Entrada HTTP | `FichasProfesionalesController` | Validar la forma de la solicitud, tomar el autor de la sesión y traducir errores. El alta y la edición pasan por `GestionarProfesionales`; la búsqueda sigue leyendo el puerto directamente. |
| Aplicación | `GestionarProfesionales` | Alta, comprobación de versión y de que cada servicio habilitado exista; omite guardados idénticos. |
| Dominio | `FichaProfesional` | Identificación, nombre, función, estado, versión y servicios habilitados. |
| Persistencia | `FichasProfesionalesPostgres`, migración V12 | Escritura condicionada por versión y auditoría en la misma transacción. |
| Autorización | `PermisosCuenta`, `SecurityConfiguration` | `PROFESIONALES_GESTIONAR` solo para ADM; todas las rutas lo exigen (reemplaza a `CUENTAS_GESTIONAR`, que usaban provisionalmente desde RF-02). |

**Concurrencia (RNF-03).** La edición ejecuta `UPDATE ... SET version = version + 1 WHERE id = ? AND version = ?`, igual que `servicios` y `consultorios`. Las altas simultáneas con la misma identificación las resuelve la restricción única de `profesionales`, ya existente desde RF-02.

**Auditoría (nota importante, distinta de servicios/consultorios).** `auditoria_profesionales` guarda el actor, la fecha, el tipo y una foto JSON, pero **esa foto no incluye el nombre ni la identificación** (RF-28/CA-2, RN-17): solo el estado, la versión y los IDs de los servicios habilitados. A diferencia de servicios y consultorios, la ficha profesional identifica a una persona real. Si falla la auditoría se revierte todo el cambio. Un guardado sin cambios no incrementa la versión ni audita.

**Migración de permiso.** Las rutas `GET`/`POST /api/profesionales/fichas` exigían `CUENTAS_GESTIONAR` desde RF-02, porque solo servían al flujo de vínculo de cuentas. Ahora exigen `PROFESIONALES_GESTIONAR`, consistente con el resto del catálogo. El cambio no afecta a ningún flujo existente: hoy solo ADM tiene cualquiera de los dos permisos.

**Lectura solo para ADM (pendiente).** Hoy `GET /api/profesionales/fichas` exige `PROFESIONALES_GESTIONAR`. Cuando se implemente RF-13 (programar citas), recepción necesitará su propia lectura de profesionales activos y habilitados para un servicio; eso no está implementado todavía.

## API

Todas las rutas exigen `PROFESIONALES_GESTIONAR` (ADM); las escrituras requieren CSRF.

| Método | Ruta | Entrada o resultado |
|---|---|---|
| GET | `/api/profesionales/fichas?busqueda=texto` | Buscar fichas por nombre/identificación (hasta 50 resultados, sin paginar; sin cambios desde RF-02). |
| GET | `/api/profesionales/fichas/{id}` | Consultar una ficha, con sus servicios habilitados. |
| POST | `/api/profesionales/fichas` | `identificacion`, `nombre`, `funcion` y `serviciosHabilitados[]` opcional. Devuelve 201 con versión 0. |
| PUT | `/api/profesionales/fichas/{id}` | Igual que el alta, más `activa` y la `version` leída. |

Errores: 400 (datos inválidos o `SERVICIO_INVALIDO`), 401 (sin sesión), 403 (sin permiso o CSRF), 404 (`NO_EXISTE`), 409 (`CONFLICTO` o identificación duplicada), 503 (fallo de persistencia).

## Verificación realizada

- `RUN_POSTGRES_TESTS=true ./mvnw clean verify`, desde backend: **132 pruebas, cero fallos y cero omisiones**.
- `npm test`, desde frontend: **55 pruebas aprobadas**. `npm run build` correcto.
- PostgreSQL (`ProfesionalesHttpTest`, esquema aislado; el alta básica y el vínculo de RF-02 siguen probados en `RolesAdminHttpTest`):
  - alta con servicios habilitados y auditoría que no contiene nombre ni identificación;
  - la edición incrementa la versión y audita el estado anterior y el nuevo;
  - una versión obsoleta recibe 409;
  - servicio inexistente al editar se rechaza con 400;
  - un guardado idéntico no audita;
  - **10 ediciones concurrentes con la misma versión → 1 aplicada, 9 `CONFLICTO`**;
  - un fallo de auditoría revierte el alta y la edición;
  - REC, CAJ, PRO y cuenta sin roles reciben 403; ADM+REC puede gestionar;
  - sin sesión 401, sin CSRF 403, rol retirado 403, cuenta desactivada 401.
- Interfaz: alta con servicios seleccionados, edición con versión preservando la selección, conflicto con recarga, duplicado, listado, visibilidad por permiso y análisis Axe (contraste excluido en jsdom).

## Pendiente (depende de requisitos fuera de alcance)

- **RF-07/CA-1:** que solo profesionales activos y habilitados reciban nuevas reservas de un servicio — depende de que exista la reserva de citas (RF-13).
- **RF-07/CA-3:** mostrar las citas futuras afectadas antes de desactivar un profesional (RN-08) — depende de que existan citas.
- **Lectura de recepción:** un contrato de solo lectura para que REC consulte profesionales activos y habilitados al programar una cita (RF-12/RF-13).

## Dónde cambiar cada comportamiento

- **Reglas de identificación, nombre, función y servicios habilitados:** `FichaProfesional.java`.
- **Versión, servicios inválidos o guardados idénticos:** `GestionarProfesionales.java`.
- **SQL, concurrencia y auditoría:** `FichasProfesionalesPostgres.java`.
- **Quién gestiona profesionales:** `PermisosCuenta.java` y las rutas en `SecurityConfiguration.java`.
- **Vínculo con cuentas (sin cambios en este RF):** `GestionarCuentas.java`, documentado en `docs/GESTION_CUENTAS.md`.
- **Nuevas columnas o restricciones:** agregar otra migración; no editar V8 ni V12.
