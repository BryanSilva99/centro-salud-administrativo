# Entrega de la funcionalidad de login

Alcance: ingreso y cierre de sesión (RF-01), interfaz React, sesión persistida, protecciones de acceso y carga de roles/permisos. Se completa esta funcionalidad para las rutas disponibles; no se atribuye implementación a los módulos de pacientes, citas, caja ni a RF-02/RF-03.

## Criterios de aceptación

| Criterio | Implementación | Evidencia |
|---|---|---|
| RF-01/CA-1: cuenta activa y acceso autorizado | Verificación de credenciales; roles REC, CAJ, ADM, PRO combinables; autorización en backend con denegación por defecto. Consulta administrativa de cuenta permitida únicamente con CUENTAS_CONSULTAR (ADM). | Pruebas HTTP de las 16 combinaciones de roles, recurso existente y UUID ajeno/inexistente; rechazo antes de consultar para no autorizados. |
| RF-01/CA-2: rechazo genérico | Cuenta inexistente/inactiva, contraseña incorrecta y bloqueo de intentos generan el mismo error. | Pruebas de verificación, PostgreSQL y HTTP. |
| RF-01/CA-3: sesión terminada no reutilizable | Logout invalida cookie, 30 minutos de inactividad, 8 horas absolutas, revocación por hash o estado. | Reutilización de cookie rechazada; límites temporales exactos; cambio de contraseña y desactivación/reactivación. |
| Cambios de rol con sesión abierta | Roles releídos de PostgreSQL antes de autorizar cada solicitud. El contexto de esa solicitud usa los permisos actuales, aunque la sesión guardada contenga autoridades anteriores. | Misma cookie: ADM permite leer; retirar ADM produce 403 inmediato; añadirlo habilita la siguiente solicitud. |
| Cliente sin autoridad sobre permisos | Roles y permisos del login provienen del servidor. Campos falsificados en JSON no conceden privilegios. | Prueba enviando ADM y CUENTAS_CONSULTAR desde un cliente REC, que recibe 403. |
| Presentación coherente | React muestra roles y permisos de la sesión y los refresca en sus comprobaciones. No mantiene un permiso visual retirado. | Pruebas React de actualización y cuenta sin roles. |

## Roles y permisos

`Rol.java` define REC, CAJ, ADM y PRO. ADM no hereda otros roles. `Permiso.java` contiene por ahora **CUENTAS_CONSULTAR**, porque es la operación administrativa ya implementada. `PermisosCuenta.java` concede ese permiso exclusivamente a ADM.

La migración V5 crea `cuenta_roles`, con relación a cuentas, clave compuesta para impedir duplicados y restricción de códigos válidos. No asigna privilegios automáticamente a cuentas existentes.

Las cuentas sin roles pueden autenticarse y consultar/cerrar su propia sesión, pero no reciben permisos administrativos. Esto evita confundir estar autenticado con estar autorizado. No se crea una jerarquía en la que administrador signifique acceso universal.

## Cambios en el recorrido

- `ConsultarRoles` es un puerto de aplicación; `ConsultarRolesPostgres` lo implementa.
- `AutorizacionSesion` transforma roles y permisos en autoridades de Spring Security.
- `ValidarSesionFilter` valida vigencia/estado/versión, carga los roles actuales y construye un contexto nuevo para cada solicitud. No modifica un contexto compartido entre solicitudes concurrentes.
- `SecurityConfiguration` exige CUENTAS_CONSULTAR para `GET /api/cuentas/{id}`; las demás rutas de negocio siguen denegadas.
- `SesionResponse` añade `roles` y `permisos` a login y `/me`, usando las mismas autoridades del contexto autorizado. `CuentaResponse` permanece separado para no publicar datos de autenticación en consultas administrativas.
- React muestra nombres de roles y permisos vigentes. No decide la autorización real: manipular su estado o esconder botones no permite eludir el backend.

## Contrato de integración para el equipo

Login y `/me` devuelven `id`, `identificador`, `activa`, `roles` y `permisos`. La sesión continúa en cookie HttpOnly, con CSRF obligatorio para escrituras.

Al implementar una nueva operación, el responsable de ese módulo debe:

1. Añadir su permiso explícito al catálogo y asignarlo de acuerdo con la matriz de requisitos.
2. Proteger la ruta en el backend sin sustituir la denegación por defecto por un comodín amplio.
3. Validar dentro de su caso de uso las restricciones de recurso, por ejemplo agenda propia de PRO o separación solicitante/aprobador.
4. Probar acceso permitido y rechazado, cambios de roles e intento de cambiar IDs.

Estas reglas pertenecen a la operación de cada módulo; el login entrega identidad y permisos vigentes, pero no puede comprobar propiedad de citas, caja o recursos todavía no implementados. No se declara completa toda la matriz del sistema.

## Cuenta local y demostración

Con migraciones aplicadas, desde `infra/local/`:

```bash
npm run cuenta -- --rol ADM
```

Asigna de forma explícita ADM a `bryan.local`; sin `--rol` el script no añade roles. La contraseña permanece en `.data/cuenta-local.json`, excluida de Git. El aprovisionamiento local no sustituye la futura pantalla administrativa ni su auditoría.

Abrir `http://127.0.0.1:5173`, iniciar sesión, comprobar el rol, recargar y cerrar sesión. Las pruebas HTTP cubren además los escenarios que no se deben provocar manualmente sobre datos de trabajo.

## Validación de esta entrega

- Backend: `RUN_POSTGRES_TESTS=true ./mvnw verify` — **42 pruebas, sin fallos ni omisiones**, contra PostgreSQL y servidor HTTP real.
- React: `npm test` — **12 pruebas aprobadas**.
- React: `npm run build` — TypeScript y producción correctos.
- La prueba de combinaciones recorre las 16 posibilidades dentro de un test; no se suma artificialmente al total de 42.

No se incluyen en esta funcionalidad el CRUD de cuentas, la interfaz de asignación de roles, protección del último administrador, recuperación/cambio de contraseña desde pantalla ni auditoría administrativa. Son RF-02 y RF-03 u operaciones adicionales. La infraestructura de revocación está disponible para su integración.

El cierre corresponde al login del proyecto en el entorno local, no a un despliegue auditado de producción. Se mantienen las limitaciones operativas documentadas en los pasos anteriores (HTTPS de despliegue, retención de intentos, controles generales de tráfico y accesibilidad integral).

## Ampliación: administración de roles

Ya existe pantalla y API para asignar los cuatro roles definidos. Ver [estado y recorrido actual](ROLES_ADMINISTRACION.md). Las cifras de pruebas anteriores corresponden al cierre del login; el documento de roles incluye la validación posterior.
