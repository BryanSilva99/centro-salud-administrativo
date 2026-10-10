# Administración de roles

Estado: implementada en Spring Boot, PostgreSQL y React. Los roles son REC (recepción/admisión), CAJ (caja), ADM (administrador del centro) y PRO (profesional de salud). Son roles de acceso, no cuatro actores nuevos. Una cuenta puede combinar varios; ADM no hereda las funciones de los demás.

## Cómo probar

Con los servicios locales iniciados, abrir http://127.0.0.1:5173/ e ingresar con la cuenta local. La cuenta `bryan.local` ya dispone de ADM. Elegir **Administrar cuentas y roles**, buscar y seleccionar una cuenta, marcar los roles y guardar. El historial aparece debajo. Si solo existe un administrador activo, no se puede retirar su rol ADM.

No se incluyen credenciales en este documento. La preparación local existente las guarda en `.data/cuenta-local.json`, excluido de Git.

## Qué hace y por qué

- **Autoriza en el servidor.** Consultar, cambiar roles y leer el historial exige ROLES_GESTIONAR, concedido exclusivamente a ADM. Ocultar la pantalla solo mejora la experiencia; no sustituye la validación del backend.
- **Aplica cambios a sesiones abiertas.** El filtro consulta los roles actuales en cada solicitud. La pantalla actualiza la sesión después de guardar y durante las comprobaciones de sesión existentes; retirar ADM elimina el panel al recibir los nuevos permisos.
- **Conserva un administrador activo.** El caso de uso rechaza quitar ADM al último administrador activo. Un ADM inactivo no sirve como sustituto.
- **Evita cambios simultáneos incompatibles.** Todas las escrituras de roles usan un bloqueo transaccional común y vuelven a comprobar el permiso del autor después de adquirirlo. Dos administradores no pueden retirarse simultáneamente y dejar cero administradores.
- **Detecta ediciones obsoletas.** La pantalla envía la versión leída. Si otra operación modificó los roles, el servidor responde con conflicto y se exige recargar antes de editar. Nunca se sobreescribe automáticamente.
- **Audita el cambio completo.** Conserva ID del autor, ID de la cuenta, roles anteriores/nuevos y fecha. La auditoría y los roles se guardan en una sola transacción: si falla cualquiera, se deshacen ambos. Reenviar exactamente los roles actuales no genera auditoría artificial.
- **Permite dejar una cuenta sin roles.** Puede iniciar sesión, pero no acceder a funciones de negocio. Esto no permite retirar el último ADM activo.
- **Consulta por páginas.** Directorio e historial devuelven hasta 20 registros por página. El directorio permite buscar por identificador.

Estas prácticas de autorización, concurrencia y auditoría aplican a muchos sistemas administrativos. Los cuatro roles y la decisión de conservar un administrador activo corresponden a este proyecto.

## Recorrido por capas

1. `RolesPanel.tsx` presenta cuentas, selección de roles e historial. `api/roles.ts` envía solicitudes con cookies y token CSRF.
2. `RolesController` interpreta la solicitud HTTP y obtiene al autor de la sesión, nunca de un ID enviado por el cliente.
3. `CambiarRoles`, dentro de la aplicación de identidad, coordina la autorización y las reglas sin depender de Spring ni SQL.
4. Los puertos `UnidadAcceso` y `DirectorioRoles` expresan las operaciones que la aplicación necesita.
5. `GestionAccesoPostgres` implementa esas operaciones con SQL, transacción y bloqueo. La migración V6 crea versión, control y auditoría.
6. `IdentityConfiguration` conecta las implementaciones. `SecurityConfiguration` limita las rutas y mantiene CSRF.

## API

| Método | Ruta | Función |
|---|---|---|
| GET | `/api/cuentas?busqueda=texto&pagina=0` | Directorio paginado |
| GET | `/api/cuentas/{id}/roles` | Cuenta, roles y versión |
| PUT | `/api/cuentas/{id}/roles` | Sustituir roles con `{ "roles": ["REC", "CAJ"], "version": 4 }` |
| GET | `/api/cuentas/{id}/roles/auditoria?pagina=0` | Historial paginado |

Errores: 400 datos inválidos; 401 sin sesión; 403 sin permiso o CSRF; 404 cuenta inexistente; 409 edición obsoleta/último administrador; 503 fallo de persistencia. Los códigos de conflicto distinguen CONFLICTO y ULTIMO_ADMIN.

## Validación

- Backend: `RUN_POSTGRES_TESTS=true ./mvnw verify`, ejecutado desde backend: **47 pruebas, cero fallos y cero omisiones**.
- Frontend: `npm test`: **17 pruebas aprobadas**; `npm run build`: compilación correcta.
- Casos cubiertos: persistencia, auditoría, versión obsoleta, ausencia de CSRF, datos inválidos, permisos retirados, cuenta sin roles, último administrador, retiro concurrente de dos administradores y reversión ante fallo de auditoría.
- Las pruebas de administración crean un esquema PostgreSQL aislado y lo eliminan al terminar; no retiran roles de la cuenta local.

## Alcance real y continuidad

La asignación de roles ya se complementó con creación, edición y activación/desactivación de cuentas, usando el mismo bloqueo para conservar un administrador activo. Ver [gestión de cuentas y validación actual](GESTION_CUENTAS.md). Las cifras de pruebas anteriores corresponden al cierre del paso de roles.

RF-02 también incluye ahora la vinculación con fichas profesionales. RF-03 (contraseñas) es independiente y sigue pendiente. El catálogo permanece fijado en los cuatro roles definidos por el proyecto.
