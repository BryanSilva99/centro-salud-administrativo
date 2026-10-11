# Catálogo de vistas y plan incremental

> Actualización funcional (11/10/2026): la rama `integration/administracion` incorpora pacientes y recursos al menú de main. El inventario histórico siguiente describe la extracción anterior; consultar [estado y comprobaciones de la integración](../INTEGRACION_ADMINISTRACION.md). Esto no declara adaptación visual de las nuevas pantallas a Figma.

Fecha de inspección: 11 de octubre de 2026. Base común: `main`, commit `d7aca93`. Referencia adicional revisada: `feature/pacientes`, commit `fe559f3`, pendiente de integrar en main.

Esta documentación es transversal al proyecto. Las referencias a código de pacientes describen la rama revisada, no funcionalidad ya disponible en main.

## Actualización de implementación visual — 11/10/2026

El árbol de trabajo de `main` incorpora la adaptación de login (23:42), estructura administrativa y directorio de cuentas (28:412), alta/edición de cuentas (28:488), roles y vínculo profesional existentes. La tabla histórica inferior describe la inspección en d7aca93; este apartado actualiza su estado visual sin considerar integrados los módulos de otras ramas.

Se mantienen `/` y navegación local, sin añadir router. SessionView muestra Inicio y Usuarios y roles según permisos; el directorio usa una tabla con identificador, roles, vínculo y estado reales. El detalle agrupa edición, roles, ficha e historiales en tarjetas. Se conservan CSRF, versiones, validaciones y permisos. Los mockups de pacientes y módulos futuros no se implementaron en main.

Inter se sirve localmente desde `frontend/public/fonts/InterVariable.ttf`, con licencia OFL incluida. El login conserva identificador y mostrar contraseña; el alta conserva contraseña y confirmación. Por ello sus campos y alturas no son una copia literal del mockup. No se añadieron filtros sin contrato, métricas, búsqueda global ni nombres/correos de persona. Se corrigió el recorte que presenta la tabla original de Figma usando flujo CSS normal. El texto de navegación seleccionada usa Primary sobre Accent para legibilidad; foco, estados deshabilitados y adaptación estrecha son decisiones de accesibilidad documentadas, no propiedades extraídas de Figma.

Verificación: login y directorio revisados a 1360×860; directorio, alta y recorrido de edición/vínculo a 1366×768; edición sin desbordamiento horizontal en viewport solicitado 390×844. Revisión en navegador integrado con respuestas sintéticas temporales, retiradas al finalizar. No se ejecutó un flujo contra PostgreSQL ni una revisión en Firefox; no se afirma conformidad total RNF-08. Las pruebas automatizadas de frontend cubren autenticación, permisos, conflictos, altas, vínculo y navegación; build valida TypeScript y empaquetado.

## Qué existe en main y qué sigue en ramas

Fuentes: [organización](../ORGANIZACION_DEL_CODIGO.md), [requisitos](../02_REQUERIMIENTOS_DEL_SISTEMA.md), `frontend/src/app/App.tsx`, `features/identity/`, `features/administration/` y controladores/configuración de seguridad del backend. React 19.3.0, TypeScript 7.0.2 y Vite 8.3.4 según package.json. No hay dependencia de router; `App` conmuta estados loading/unavailable/anonymous/authenticated y `SessionView` despliega paneles locales. **La única ubicación de navegación implementada es `/`**; una ruta HTTP `/api/...` no es una ruta de pantalla.

Main contiene identidad, cuentas/roles y ficha profesional básica. El registro de pacientes existe en `feature/pacientes` y fue probado en `integration/pacientes`, pero aún no se ha integrado en main. Las carpetas de appointments, billing, reporting y audit no equivalen a vistas implementadas. El código de otras ramas no se considera disponible en main; se identifica expresamente cuando se usa como referencia. La función existente no prueba que su diseño ya coincida con Figma.

## Correspondencia completa

“Futura” = sin vista ni operación integrada en main; no se propone URL nueva. “Parcial” identifica funcionalidad relacionada, no cumplimiento completo del frame. El rol indica el alcance de la pantalla diseñada y debe respetar la matriz de requisitos; no crea un permiso nuevo.

| Frame Figma | Rol | Requerimiento / propósito | React actual | Ruta y estado |
|---|---|---|---|---|
| [00 · Sistema visual](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-9) | — | Fundamentos y componentes | — | No aplica; referencia visual |
| [Acceso · Login](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-42) | Todos | RF-01 | LoginForm | `/`; función existente, diseño pendiente |
| [Inicio · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-72) | REC | RF-25; accesos a RF-04/05/13/16 | — | Sin ruta; futura |
| [Inicio · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-185) | CAJ | RF-20/24; indicadores económicos propios | — | Sin ruta; futura |
| [Inicio · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-288) | ADM | RF-25; supervisión y RF-23 | — | Sin ruta; futura |
| [Mi inicio · PRO](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-397) | PRO | RF-25; agenda propia | — | Sin ruta; futura |
| [Pacientes · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-497) | REC | RF-05; búsqueda y selección | — | Sin ruta; futura |
| [Agenda y citas · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-598) | REC | RF-12; agenda | — | Sin ruta; futura |
| [Registrar paciente · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-22) | REC | RF-04 | RegistrarPacienteForm (feature/pacientes) | `/` en la rama; pendiente de integración y adaptación visual |
| [Editar paciente · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-99) | REC | RF-05; edición | — | Sin ruta; futura |
| [Detalle del paciente · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-176) | REC | RF-05/06; consulta administrativa | — | Sin ruta; futura |
| [Historial administrativo · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-259) | REC | RF-06; historial de citas permitido | — | Sin ruta; futura |
| [Disponibilidad · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-326) | REC | RF-12; horarios disponibles | — | Sin ruta; futura |
| [Programar cita · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-416) | REC | RF-13 | — | Sin ruta; futura |
| [Reprogramar cita · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-489) | REC | RF-14 | — | Sin ruta; futura |
| [Cancelar cita · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-555) | REC | RF-15 | — | Sin ruta; futura |
| [Admisión del día · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-611) | REC | RF-16/18 | — | Sin ruta; futura |
| [Sala de espera · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-706) | REC | RF-16/18; retiro de espera | — | Sin ruta; futura |
| [Mi agenda / espera · PRO](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-791) | PRO | RF-12/17; solo propias | — | Sin ruta; futura |
| [Profesionales · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-43) | ADM | RF-07; directorio completo | Ficha parcial | `/`; funcionalidad parcial, diseño pendiente |
| [Formulario profesional · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-136) | ADM | RF-07; perfil completo | Ficha parcial | `/`; funcionalidad parcial, diseño pendiente |
| [Servicios · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-216) | ADM | RF-08 | — | Sin ruta; futura |
| [Formulario servicio · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-309) | ADM | RF-08 | — | Sin ruta; futura |
| [Consultorios · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-383) | ADM | RF-09 | — | Sin ruta; futura |
| [Formulario consultorio · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-476) | ADM | RF-09 | — | Sin ruta; futura |
| [Turnos y disponibilidad · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-553) | ADM | RF-10 | — | Sin ruta; futura |
| [Bloqueos y ausencias · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-658) | ADM | RF-11; impacto en citas | — | Sin ruta; futura |
| [Cargos y saldos · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-748) | CAJ | RF-19/22 | — | Sin ruta; futura |
| [Registrar cargo · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-839) | CAJ | RF-19 | — | Sin ruta; futura |
| [Registrar pago · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-910) | CAJ | RF-21 | — | Sin ruta; futura |
| [Apertura de caja · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-68) | CAJ | RF-20 | — | Sin ruta; futura |
| [Sesión de caja abierta · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-117) | CAJ | RF-20/21/22 | — | Sin ruta; futura |
| [Movimientos de caja · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-188) | CAJ | RF-22 | — | Sin ruta; futura |
| [Cierre y conciliación · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-259) | CAJ | RF-24 | — | Sin ruta; futura |
| [Solicitud de reversión · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-308) | CAJ | RF-23; solicitar | — | Sin ruta; futura |
| [Resolver reversión · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-367) | ADM | RF-23; aprobar/rechazar, actor distinto | — | Sin ruta; futura |
| [Cuentas y roles · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-412) | ADM | RF-02 | RolesPanel | `/`; función existente, diseño pendiente |
| [Crear / editar cuenta · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-488) | ADM | RF-02 | FormulariosCuenta | `/`; función existente, diseño pendiente |
| [Gestión de roles · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-542) | ADM | RF-02; roles predefinidos | RolesPanel parcial | `/`; funcionalidad parcial, diseño pendiente |
| [Reportes operativos · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-608) | ADM | RF-26 · F16 | — | Sin ruta; futura |
| [Reportes económicos · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-680) | ADM | RF-27 | — | Sin ruta; futura |
| [Auditoría · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-743) | ADM | RF-28; auditoría global | Auditoría parcial | `/`; funcionalidad parcial, diseño pendiente |
| [Exportación autorizada · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-816) | ADM | RF-29 · F16 | — | Sin ruta; futura |
| [Estados operativos · patrones](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-867) | Según operación | Vacío, carga, éxito, error y advertencia | — | No aplica; referencia visual |

## Componentes existentes y límites

| Componente / archivo relativo a frontend/src | Función actual | Límite frente al mockup |
|---|---|---|
| `app/App.tsx` | Estado de sesión, layout, error de conexión y reintento | No sidebar ni dashboard por rol |
| `features/identity/components/LoginForm.tsx` | Acceso por identificador/contraseña | No autentica por un campo nuevo de correo |
| `features/identity/components/SessionView.tsx` | Roles, logout y apertura de paneles autorizados | No es una de las cuatro pantallas Inicio de Figma |
| `features/identity/components/RolesPanel.tsx` | Buscar cuentas, seleccionar, asignar roles y consultar cambios | No directorio de pacientes; no editor de definiciones de permisos |
| `features/identity/components/FormulariosCuenta.tsx` | Crear/actualizar cuenta | Mantener identificador y contraseña de alta; no nombre/correo nuevos |
| `features/identity/components/VinculoProfesional.tsx` | Buscar/registrar ficha básica y vincularla | No gestión completa de servicios, colegiatura y disponibilidad RF-07 |
| `features/administration/components/RegistrarPacienteForm.tsx` | Alta RF-04 en feature/pacientes | No existe en main; sin búsqueda, edición ni historial RF-05/06 |
| `features/identity/api/*.ts`, `features/administration/api/pacientes.ts` | Identidad en main; pacientes en feature/pacientes | No crear operaciones al copiar botones de Figma |
| `shared/api/http.ts`, `identity/api/auth.ts` | Peticiones, errores, CSRF, sesión | Reutilizar comportamiento; una adaptación visual no redefine seguridad |

## Contratos disponibles para adaptar vistas actuales

Inventario, no propuesta de API. Los endpoints de identidad están en main; el POST de pacientes pertenece únicamente a la rama pendiente de integrar. La autorización se mantiene en servidor y se refleja en interfaz.

| Operación actual | Endpoint existente | Acceso relevante |
|---|---|---|
| Login, sesión, actividad y logout | POST `/api/auth/login`, GET `/api/auth/me`, POST `/api/auth/activity`, POST `/api/auth/logout`; GET `/api/auth/csrf` | Sesión/CSRF según operación; login recibe identificador y contrasena |
| Registro de pacientes | POST `/api/pacientes` | Solo feature/pacientes: PACIENTES_GESTIONAR, asignado a REC |
| Buscar cuentas | GET `/api/cuentas` | ROLES_GESTIONAR |
| Crear/actualizar cuenta | POST `/api/cuentas`, PUT `/api/cuentas/{id}` | CUENTAS_GESTIONAR |
| Roles e historial | GET/PUT `/api/cuentas/{id}/roles`, GET `/api/cuentas/{id}/roles/auditoria` | ROLES_GESTIONAR |
| Historial de cuenta | GET `/api/cuentas/{id}/auditoria` | CUENTAS_GESTIONAR |
| Fichas básicas | GET/POST `/api/profesionales/fichas`, GET `/api/profesionales/fichas/{id}` | CUENTAS_GESTIONAR |
| Vínculo profesional | PUT `/api/cuentas/{id}/profesional` | CUENTAS_GESTIONAR |

Main no tiene endpoints de pacientes. La rama feature/pacientes solo añade POST, no GET de directorio. Mantener versiones de edición en cuentas/roles/vínculos. Los permisos CUENTAS_CONSULTAR, CUENTAS_GESTIONAR y ROLES_GESTIONAR corresponden a ADM; PACIENTES_GESTIONAR a REC únicamente en feature/pacientes, hasta su integración. Una cuenta puede combinar roles, pero ADM no hereda recepción/caja. Los mockups futuros no autorizan añadir nuevos nombres de permisos al código.

## Plan incremental (documentación, sin implementación en esta tarea)

Cada etapa debe ser revisable por separado. Mantener las reglas del backend y no mezclar cambios visuales con ampliaciones funcionales.

| Etapa | Trabajo acotado | Dependencia y criterio de cierre |
|---|---|---|
| 0. Referencia completada | Colores, fuentes, medidas, componentes, recursos y ausencia de variantes verificados; evidencia JSON y cuatro renders inspeccionados | Extracción cerrada el 11/10/2026. Conservar ambas familias de sidebar por pantalla; no confundir estados no diseñados con datos pendientes |
| 1. Fundamentos visuales | Mapear tokens y estilos verificados al CSS existente, con la mínima reutilización necesaria | Paleta exacta, fuente comprobada y componentes comparados con Figma; no nueva biblioteca por defecto |
| 2. Login | Adaptar 23:42 manteniendo identificador, contraseña, validación, sesión y mensajes actuales | Captura al viewport de referencia; login correcto/incorrecto, indisponibilidad y teclado sin regresión |
| 3. Navegación de funciones disponibles tras integración | Separar accesos a cuentas y registro de pacientes siguiendo estructura Figma resuelta | No mostrar dashboards ficticios ni enlaces que prometan operaciones no disponibles; verificar REC, ADM, ADM+REC y cuenta sin roles. Cualquier estrategia de rutas se decidirá explícitamente: hoy solo existe `/` |
| 4. Alta de pacientes | Primero integrar RF-04 mediante su proceso separado y pruebas; después adaptar 24:22 y conservar sus grupos visuales con los campos reales | Alta confirmada y código, duplicado, inválidos, doble envío, error/red y sesión vencida; sin Dirección ni otras ampliaciones |
| 5. Cuentas y vínculo | Adaptar 28:412/488/542 conservando límites de RF-02 y ficha básica | Crear/editar, roles combinados, control de versión, restricciones y vínculo comprobados; no simular RF-07 completo ni editor de permisos |
| 6. Futuras vistas de pacientes | 23:497, 24:99/176/259 | Solo tras implementar y validar RF-05/06 con autorización independiente; consultas paginadas, edición concurrente y campos por rol |
| 7. Recursos y citas | Frames 25:43–658 y 23:598, 24:326–791; inicio operativo cuando existan datos | Dependen de pacientes, recursos, disponibilidad y contratos integrados. Verificar estados/transiciones, ámbito PRO y conflictos; no tomar mocks como datos finales |
| 8. Caja y supervisión | Frames económicos y reversión; inicio CAJ/ADM según alcance | Backend integrado y validado: saldo, idempotencia, caja propia, solicitante/aprobador distintos y cierre. No habilitar solo por existir el diseño |
| 9. Reportes y auditoría global | 28:680/743; luego 28:608/816 | Auditoría de cuentas ya disponible no equivale a auditoría global. RF-26 y RF-29 son F16: diferir reporte detallado/exportación hasta autorización y contrato |

La secuencia no obliga a terminar todas las pantallas de una etapa en un solo cambio. Las pantallas futuras se describen porque existen en Figma/requisitos, no porque esta documentación autorice implementarlas.

## Evidencia y actualización del estado

Por vista registrar nodo, commit, fecha, rol, estado funcional y visual por separado, operaciones reales, capturas de comparación y resultados de pruebas. Para cambios futuros ejecutar `npm test` y `npm run build`, pruebas backend si cambia el contrato, y recorrido real del flujo. RNF-08 exige teclado, foco, errores por campo, estados no basados solo en color, Chrome/Firefox y viewport 1366 × 768. Axe complementa, no sustituye, revisión manual. No se ejecutaron pruebas de aplicación en esta tarea documental ni se afirma equivalencia visual.

Ver [patrones y discrepancias](PATRONES_UX.md), [sistema visual](DESIGN_SYSTEM.md) y [referencias](FIGMA_REFERENCIAS.md).
