# Patrones UX y límites funcionales

Fecha de inspección: 11 de octubre de 2026. Base común: `main`, commit `d7aca93`. Referencia adicional revisada: `feature/pacientes`, commit `fe559f3`, pendiente de integrar en main.

Esta documentación es transversal al proyecto. Las referencias a código de pacientes describen la rama revisada, no funcionalidad ya disponible en main.

## Regla de decisión

1. Mantener los campos, operaciones y permisos que ya admite el código, respaldados por los [requisitos](../02_REQUERIMIENTOS_DEL_SISTEMA.md).
2. Usar Figma para apariencia, jerarquía y composición. No añadir endpoints, permisos, pantallas inventadas ni datos requeridos porque aparezcan dibujados.
3. Cuando el mockup contradiga el comportamiento actual, documentar la excepción y conservar el comportamiento. Una necesidad futura de requisitos se implementa en otra tarea autorizada, no como cambio de estilos.
4. No presentar contenido de demostración como registros reales ni cifras operativas. Los nombres y cantidades del mockup no son fixtures de producción.

## Navegación y permisos

Figma muestra sidebar por rol, topbar, breadcrumb y entradas de módulos; React hoy abre paneles dentro de `/`. La adaptación debe distinguir registro de pacientes de ficha profesional y gestión de cuentas. No convertir el rol en un selector que conceda permisos: la sesión y el backend determinan el acceso. Las cuentas multirrol conservan la unión de permisos sin herencia implícita ADM→REC/CAJ.

Mantener cierre de sesión, expiración y estado sin roles visibles. No habilitar el buscador global “Buscar…” de los mockups como una operación nueva: no hay contrato de búsqueda global. Un enlace a una futura pantalla no debe parecer operativo. El mockup no define un estado deshabilitado para esos enlaces; mostrar únicamente accesos disponibles evita prometer operaciones inexistentes.

## Formularios

**Patrón visual observado:** título, descripción, grupos de campos etiquetados, acciones cancelar/guardar; obligatorios marcados en algunos frames. **Comportamiento actual a preservar:** validación frontend y backend, CSRF en escrituras, protección contra doble envío, errores comprensibles, confirmación únicamente tras respuesta válida. No reintentar automáticamente una alta cuya respuesta se perdió.

### Registro de pacientes (24:22 → RF-04, rama pendiente de integrar)

Las reglas, respuestas y componentes de esta sección fueron verificados en feature/pacientes. No están disponibles todavía en main. Se conservan como contrato de referencia para después de la integración, no como autorización para implementarlo de nuevo.

| Campo real | Regla actual |
|---|---|
| Tipo de documento | DNI o CE |
| Número | DNI: 8 dígitos; CE: 9 caracteres alfanuméricos, normalizado a mayúsculas |
| Nombres / apellidos | Obligatorios, hasta 100 caracteres por campo |
| Fecha de nacimiento | Obligatoria; 1900-01-01 hasta hoy según Lima en backend |
| Teléfono / correo | Opcionales; se validan por forma, no acreditan identidad |
| Código / ID | Asignados por el sistema; no se capturan como elección del usuario |

Dirección no existe en el DTO ni formulario actual: excluirla de la adaptación funcional. El mockup no marca fecha como obligatoria, pero sí lo es. El frontend hoy calcula la fecha máxima con la zona del navegador; registrar esta diferencia respecto del backend de Lima para una tarea separada, sin modificarla en esta documentación.

Tras éxito mostrar el código y limpiar el formulario, como hace la implementación. Ante `DOCUMENTO_DUPLICADO`, mostrar el mensaje de registro existente y no insertar otro paciente. La búsqueda para recuperar ese registro aún no existe: no añadir un botón que simule ese flujo. En fallo de red conservar los datos y advertir de resultado incierto antes de repetir. Ante 401 volver al acceso; ante 403 informar falta de permiso. El código actual muestra error general; los errores junto al campo exigidos por RNF-08 son una brecha de UX a resolver en la adaptación autorizada.

### Cuentas, roles y ficha profesional

Conservar identificador, estado, roles, contraseña de alta y versión de actualización. Cambiar roles o estado se valida nuevamente en servidor; ocultar botones no sustituye autorización. Conservar el conflicto de versión y la posibilidad de recargar sin sobrescribir cambios ajenos. La ficha básica permite identificación, nombre y función; vincularla no implementa servicios habilitados ni turnos.

## Tablas y listas

Figma presenta encabezados, filas alternas, badges, filtros y paginación. Actualmente cuentas utiliza una lista seleccionable y controles anterior/siguiente con `hayMas`; no hay total global en ese contrato. No inventar “128 resultados”, números de página finales o contadores del mockup. Al adaptar a tabla mantener selección y operaciones reales, etiquetas de columna, controles de teclado y estados de carga, vacío y error diferenciados.

Directorio/edición de pacientes dependen de RF-05; historial de RF-06. No utilizar el listado de cuentas como directorio de pacientes. CAJ solo recibe identificación mínima necesaria para cobro; PRO solo la de sus citas. ADM por sí solo no puede consultar el directorio completo de pacientes.

## Estados e interacciones

El frame 28:867 define patrones visuales con los textos “No hay resultados”, “Cargando información…”, “Cambios guardados”, “No se pudo completar” y “Confirma la operación”. Son ejemplos; adaptar el texto al resultado real, sin reutilizar “La cuenta se actualizó” para un paciente.

| Estado | Regla verificable |
|---|---|
| Cargando / enviando | Mostrar avance y evitar doble envío; no mostrar éxito antes de persistir |
| Vacío | Distinguir cero resultados de error o falta de permisos; limpiar filtros solo si existen |
| Éxito | Confirmación de API y dato identificador cuando corresponda; `role=status` o anuncio equivalente |
| Error de validación | Conservar valores, asociar error al campo y mantener un resumen entendible |
| Error de red | No afirmar que no se guardó; evitar reintento automático de escritura |
| Sesión vencida / permiso retirado | Mantener respuesta real de seguridad; no mostrar datos obsoletos como autorizados |
| Conflicto concurrente | No sobrescribir silenciosamente; recargar/consultar mediante operación existente |
| Acción sensible futura | Motivo, contexto y confirmación conforme RF; un modal no habilita operación sin permiso |

La inspección confirmó cero reacciones de prototipo y cero component sets en Mockups: no hay variantes de hover, disabled o foco ni transiciones de apertura/cierre o abandono definidas. El frame 28:867 sí contiene estados estáticos, con propiedades registradas en DESIGN_SYSTEM. Las reglas anteriores proceden de requisitos/código, no de un prototipo verificado. No introducir confirmaciones adicionales por mera suposición.

## Discrepancias Figma → decisión funcional

| Frame | Diferencia observada | Decisión para adaptación |
|---|---|---|
| 23:42 Login | Correo institucional | Conservar Identificador; no exigir email ni cambiar autenticación |
| 24:22/99 Pacientes | Dirección; fecha sin asterisco | Excluir Dirección; mantener fecha obligatoria y reglas actuales |
| 23:497 Directorio | Activo/Inactivo y total de demostración | No añadir estado al paciente ni inventar totales; contrato de búsqueda aún pendiente |
| 24:259 Historial REC | Incluye “Pago registrado” e importe | No exponer historial económico a REC; matriz reserva datos económicos a CAJ/ADM |
| 24:706 Sala de espera | Propone ausencia desde espera; “Llamado” | Ausencia solo para PROGRAMADA sin llegada y tras fin; retiro para EN_ESPERA. No crear estado LLAMADO |
| 23:36, 24:489 y otras citas | Confirmada, Llegó, Completada | No crear enums a partir de etiquetas. Resolver correspondencia con PROGRAMADA, EN_ESPERA, EN_ATENCION, ATENDIDA, CANCELADA, AUSENTE al integrar citas |
| 24:416 Programación | No muestra explícitamente modalidad económica; añade motivo administrativo | Contrato validado manda: conservar modalidad/importe derivados y no añadir campos por apariencia |
| 25:658 Bloqueos ADM | Acciones “Reprogramar” | ADM aislado no programa/reprograma; resolver impacto conforme matriz y roles reales |
| 25:136 Profesional | Colegiatura, correo, servicios, especialidad seleccionable | Ficha básica actual no soporta todo RF-07; conservar identificación/nombre/función y vínculo |
| 25:839 Cargo | Concepto e importe capturados libremente | RF-19 exige cargo vinculado a cita y su condición económica; no diseñar un cobro libre |
| 28:488 Cuenta | Nombre completo y correo; no muestra contraseña inicial | Conservar contrato identificador/contrasena/roles y edición de estado. No añadir identidad personal ni retirar datos requeridos |
| 28:542 Roles | Catálogo de permisos y filtros | Roles predefinidos; no editor de permisos o creación de roles nuevos |
| 28:367 Reversión | Decisión aprobar/rechazar con botón “Aprobar” | Cuando se implemente, acción debe corresponder a decisión; ADM distinto al solicitante |
| 28:608/680/816 Reportes | Exportar autorizado | Exportación RF-29 es F16 y carece de contrato aquí; no habilitarla por copiar el botón |
| Frames con búsqueda global y cifras | Control y datos de demostración | No endpoint global ni métricas ficticias en producción |

## Accesibilidad y verificación

RNF-08 manda: completar registro, reserva y pago por teclado; foco visible; etiquetas y errores asociados; información no basada solo en color. Conservar HTML semántico, nombres accesibles, `aria-expanded` en paneles y anuncios de resultado; si se implementan diálogos, verificar foco inicial, recorrido y retorno al disparador sin inventar un modal donde el frame no lo define.

En pruebas futuras revisar orden de lectura, tabulación, campos date/select, zoom, contraste con los colores **efectivamente extraídos**, textos largos y estados vacíos/error. No existe medición de contraste Figma en esta tarea: no declarar conformidad. La documentación de la rama registra pruebas Axe básicas, que no sustituyen revisión manual. Verificar Chrome y Firefox y 1366 × 768 conforme requisitos, además del viewport Figma. Móvil/tablet y breakpoints no están definidos en los frames inspeccionados; no son propiedades bloqueadas por la extracción.

## Condición de aceptación por vista

- Nodo individual y propiedades referenciados; pendientes que afecten al cambio resueltos.
- Campos, operaciones y permisos coinciden con backend/requisitos; excepciones anteriores respetadas.
- Capturas de Figma y React comparables, con discrepancias registradas y sin datos reales.
- Flujo autorizado, rechazo sin permisos, validación, error, éxito y concurrencia relevante probados.
- No se declara implementado un módulo completo cuando solo existe su alta o ficha básica.

Plan y estados de cada vista: [CATALOGO_VISTAS](CATALOGO_VISTAS.md). Propiedades y límites de extracción: [DESIGN_SYSTEM](DESIGN_SYSTEM.md) y [FIGMA_REFERENCIAS](FIGMA_REFERENCIAS.md).
