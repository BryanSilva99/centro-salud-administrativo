# Referencias de Figma

Fecha de inspección: 11 de octubre de 2026. Base común: `main`, commit `d7aca93`. Referencia adicional revisada: `feature/pacientes`, commit `fe559f3`, pendiente de integrar en main.

Esta documentación es transversal al proyecto. Las referencias a código de pacientes describen la rama revisada, no funcionalidad ya disponible en main.

## Fuente y alcance de la inspección

Archivo `CfWGA410YlwAtGY1FBJRVK`, página `23:2`, **02 · Mockups**. Se recuperaron metadatos de la página y nombres, dimensiones, jerarquía y textos de sus 44 frames mediante Figma MCP. Los textos se consultaron en dos lotes de 22 frames; no se afirma haber leído cada propiedad de cada nodo.

La extracción de propiedades se completó mediante consultas compactas de solo lectura el 11 de octubre de 2026. El bloqueo anterior de revisión automática por cuota ya no impide esta tarea. Se inspeccionaron rellenos, fuentes, radios y efectos de los 3306 nodos descendientes de Mockups; componentes, estilos locales, variables, constraints y reacciones. Se ampliaron medidas de login, botones, tablas, formularios y estados. No se modificó Figma.

Se obtuvieron e inspeccionaron renders individuales de login (23:42), registro de pacientes (24:22), cuenta (28:488) y estados (28:867), a 1360×860. Esto permite verificar composición y diferencias entre familias; no constituye una comparación píxel a píxel con React ni una revisión visual de los 44 frames. Los enlaces temporales de imagen no se conservan como referencias permanentes.

## Índice completo de frames

Los enlaces identifican nodos vivos, no una versión inmutable del archivo. Esta tabla registra el estado observado en la fecha indicada. Todos los frames de pantalla miden 1360 × 860; el sistema visual mide 1360 × 420. Las medidas proceden de metadatos, no de estimación visual.

| Nodo | Frame y enlace directo | Propósito / referencia funcional |
|---|---|---|
| `23:9` | [00 · Sistema visual](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-9) | Fundamentos y componentes |
| `23:42` | [Acceso · Login](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-42) | RF-01 |
| `23:72` | [Inicio · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-72) | RF-25; accesos a RF-04/05/13/16 |
| `23:185` | [Inicio · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-185) | RF-20/24; indicadores económicos propios |
| `23:288` | [Inicio · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-288) | RF-25; supervisión y RF-23 |
| `23:397` | [Mi inicio · PRO](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-397) | RF-25; agenda propia |
| `23:497` | [Pacientes · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-497) | RF-05; búsqueda y selección |
| `23:598` | [Agenda y citas · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=23-598) | RF-12; agenda |
| `24:22` | [Registrar paciente · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-22) | RF-04 |
| `24:99` | [Editar paciente · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-99) | RF-05; edición |
| `24:176` | [Detalle del paciente · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-176) | RF-05/06; consulta administrativa |
| `24:259` | [Historial administrativo · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-259) | RF-06; historial de citas permitido |
| `24:326` | [Disponibilidad · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-326) | RF-12; horarios disponibles |
| `24:416` | [Programar cita · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-416) | RF-13 |
| `24:489` | [Reprogramar cita · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-489) | RF-14 |
| `24:555` | [Cancelar cita · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-555) | RF-15 |
| `24:611` | [Admisión del día · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-611) | RF-16/18 |
| `24:706` | [Sala de espera · REC](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-706) | RF-16/18; retiro de espera |
| `24:791` | [Mi agenda / espera · PRO](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=24-791) | RF-12/17; solo propias |
| `25:43` | [Profesionales · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-43) | RF-07; directorio completo |
| `25:136` | [Formulario profesional · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-136) | RF-07; perfil completo |
| `25:216` | [Servicios · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-216) | RF-08 |
| `25:309` | [Formulario servicio · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-309) | RF-08 |
| `25:383` | [Consultorios · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-383) | RF-09 |
| `25:476` | [Formulario consultorio · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-476) | RF-09 |
| `25:553` | [Turnos y disponibilidad · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-553) | RF-10 |
| `25:658` | [Bloqueos y ausencias · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-658) | RF-11; impacto en citas |
| `25:748` | [Cargos y saldos · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-748) | RF-19/22 |
| `25:839` | [Registrar cargo · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-839) | RF-19 |
| `25:910` | [Registrar pago · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=25-910) | RF-21 |
| `28:68` | [Apertura de caja · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-68) | RF-20 |
| `28:117` | [Sesión de caja abierta · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-117) | RF-20/21/22 |
| `28:188` | [Movimientos de caja · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-188) | RF-22 |
| `28:259` | [Cierre y conciliación · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-259) | RF-24 |
| `28:308` | [Solicitud de reversión · CAJ](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-308) | RF-23; solicitar |
| `28:367` | [Resolver reversión · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-367) | RF-23; aprobar/rechazar, actor distinto |
| `28:412` | [Cuentas y roles · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-412) | RF-02 |
| `28:488` | [Crear / editar cuenta · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-488) | RF-02 |
| `28:542` | [Gestión de roles · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-542) | RF-02; roles predefinidos |
| `28:608` | [Reportes operativos · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-608) | RF-26 · F16 |
| `28:680` | [Reportes económicos · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-680) | RF-27 |
| `28:743` | [Auditoría · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-743) | RF-28; auditoría global |
| `28:816` | [Exportación autorizada · ADM](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-816) | RF-29 · F16 |
| `28:867` | [Estados operativos · patrones](https://www.figma.com/design/CfWGA410YlwAtGY1FBJRVK/Sistema-de-Gestion-Administrativa-de-Centro-de-Salud?node-id=28-867) | Vacío, carga, éxito, error y advertencia |

## Evidencia local de la extracción

Los archivos siguientes registran fecha, archivo/página Figma y propiedades reales por nodo. Son evidencia documental; no tokens de producción ni código generado.

| Evidencia | Cobertura |
|---|---|
| [fundamentos.json](evidencias/fundamentos.json) | Ocho muestras de color, tipografía efectiva y ejemplos de botones/badges del frame 23:9 |
| [inventario.json](evidencias/inventario.json) | Todos los nodos de Mockups: tipos, combinaciones tipográficas, rellenos, radios, efectos, componentes y reacciones; estilos y variables locales del archivo |
| [componentes.json](evidencias/componentes.json) | Botones fuente, login, sidebar/topbar, tabla, badges y estructura tardía: bordes, medidas, padding, alineación y constraints |
| [formularios.json](evidencias/formularios.json) | Registro/edición de pacientes, cuenta, estados; geometría con ID del padre y conteos de layout/constraints de la página |

## Lo que el archivo no define

- No hay variables locales, estilos locales de pintura/texto/efecto ni component sets. Los tres componentes son Button / Primary, Secondary y Danger.
- No hay nodos vectoriales o rellenos de imagen en Mockups: iconografía y marca se componen de texto y rectángulos. No existe un paquete externo de iconos que extraer.
- No hay reacciones de prototipo ni variantes hover, foco o disabled. Hay patrones estáticos en 28:867, que no especifican transiciones.
- Los 44 frames son desktop; no proporcionan breakpoints ni pantallas móvil/tablet. Los valores responsive actuales pertenecen al código, no a Figma.
- La fuente identificada es Inter; el documento no entrega su binario o licencia a través de estas consultas.

Estas ausencias no quedan etiquetadas como extracción pendiente. No se inventan propiedades para cubrirlas. Las dos familias de estructura quedan descritas por separado en DESIGN_SYSTEM; una futura unificación sería una decisión de diseño, no una corrección de datos faltantes.

## Mantenimiento y evidencia

Por cada adaptación registrar: commit de código, fecha, nodo, dimensiones, propiedades consultadas, captura del frame y captura de React al mismo viewport, diferencias funcionales autorizadas, pruebas y pendientes. Guardar únicamente recursos de diseño permitidos y datos sintéticos; nunca credenciales o datos reales. Si el frame cambia, actualizar esta referencia y revisar sus consumidores antes de extender el cambio.

Ver [sistema visual](DESIGN_SYSTEM.md), [catálogo y plan](CATALOGO_VISTAS.md) y [patrones funcionales](PATRONES_UX.md).
