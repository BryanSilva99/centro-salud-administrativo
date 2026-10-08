# Presentación parcial: requisitos y preparación

Fuente: PRA_Proyecto_curso_TAS_2026-II.pdf, puntos 1 a 7 y notas 1 y 2. Releído el 6 de octubre de 2026. Este archivo distingue obligaciones del curso de metas propuestas para preparar el parcial.

## Requisitos explícitos

- Parcial en la semana 8; final en la semana 16.
- Cada presentación dura como máximo 20 minutos.
- Enviar diapositivas antes de exponer.
- Después de cada exposición, enviar la presentación actualizada por correo electrónico. La nota 1 indica que, de no hacerlo, no habrá nota de exposición.
- Avanzar semanalmente y validar consultas o impedimentos con el profesor.
- Validar el caso de negocio con el profesor antes de iniciar el desarrollo.
- El proyecto debe resolver un problema real mediante una aplicación funcional, con diseño nativo para nube, arquitectura Cebolla, Hexagonal o Limpia y uso de asistente de código con IA.
- Implementar seis tendencias del Radar de abril de 2026: dos del grupo denominado “Technologies” en los lineamientos, dos Tools, una Platform y una Language and frameworks. La selección del proyecto interpreta el primer grupo como Técnicas, que es el nombre del cuadrante del Radar.
- Los lineamientos indican grupos de hasta cinco integrantes. La modalidad individual considerada por el usuario no se da por aprobada en este documento.
- Debe haber al menos un representante del grupo en las exposiciones de los demás grupos; las inasistencias tienen penalización.

## Contenido que se pide para cada exposición

| Elemento requerido | Preparación para nuestro proyecto | Evidencia que debe existir |
|---|---|---|
| Descripción del proyecto | Problema administrativo concreto, usuarios, beneficio esperado y alcance | Caso de negocio validado; distinguir beneficios esperados de resultados medidos |
| Principales requerimientos | Requisitos funcionales y atributos de calidad priorizados | Requisitos identificados y criterios de aceptación |
| Tendencias y cómo se han implementado | Explicar las seis elecciones con su uso efectivo | Código/configuración y ejemplos de ejecución; distinguir pendiente de implementado |
| Matriz de tendencias vs requerimientos | Relacionar cada tendencia con los requisitos que ayuda a satisfacer | Matriz con requisitos identificados, justificación y evidencia |
| Desarrollo del ADD: contexto, arquitectura y vistas | Documentar decisiones y representar el sistema y sus límites | ADD coherente con los diagramas y con la implementación; el C4 por sí solo no cubre toda la explicación |
| Ejecución del software | Demostración de un recorrido del negocio | Software funcionando y datos ficticios preparados |
| Conclusiones del uso de las tendencias | Beneficios, dificultades, límites y aprendizajes observados | Conclusiones sustentadas en lo realmente realizado hasta el parcial |

El documento permite variaciones de los puntos según las tendencias seleccionadas, pero no ofrece una lista reducida específica para el parcial. Tampoco fija porcentajes de avance, número mínimo de módulos, puntajes por criterio o un requisito explícito de despliegue público para la semana 8. No inventar esas condiciones ni afirmar que exige el producto completo en el parcial. Preparar todos los puntos con evidencia del avance real.

## Meta de trabajo propuesta para el parcial (no requisito textual)

El detalle vigente de la meta P8, sus dependencias y demostraciones está en [02_REQUERIMIENTOS_DEL_SISTEMA.md](02_REQUERIMIENTOS_DEL_SISTEMA.md). Incluye catálogo de servicios y caja con cargos, pagos, anulaciones autorizadas y cierre, además de la operación de atención. Los reportes detallados y exportaciones se priorizan para F16. Esta distribución requiere estimación de tareas para su planificación.

Preparar un avance sustancial acorde con ocho semanas de trabajo. El flujo de reserva será un primer incremento, no la meta completa del parcial. El alcance propuesto comprende usuarios y permisos; registro, edición y búsqueda de pacientes; profesionales, consultorios y disponibilidad; programación, reprogramación y cancelación de citas con prevención de conflictos; admisión y atención básica; dashboard operativo; y aplicación verificable de las seis tendencias. Se concretará mediante requerimientos y planificación según la modalidad de trabajo. Esta es una meta propuesta, no un alcance funcional impuesto literalmente por los lineamientos.

Selección vigente: Zero Trust Architecture, Mutation testing, OpenAI Codex, Axe-core, SigNoz y React JS. La [matriz completada](03_MATRIZ_TENDENCIAS_REQUERIMIENTOS.md) define aplicación prevista y evidencias EV-01 a EV-06; la implementación sigue pendiente.

No presentar documentación generada con Codex como prueba de que el código del sistema ya se desarrolló con él. No presentar instalaciones aisladas de herramientas como evidencia suficiente de integración funcional.

## Guion orientativo de 19 minutos

| Tiempo | Contenido |
|---|---|
| 2 min | Descripción y problema del negocio |
| 2 min | Requerimientos prioritarios |
| 4 min | ADD: contexto, arquitectura y vistas |
| 4 min | Seis tendencias, implementación y matriz |
| 5 min | Ejecución del software y evidencias |
| 2 min | Conclusiones y pendientes |

Queda un minuto de margen respecto al máximo de 20. Este reparto es orientativo; no presupone cómo se organizarán las preguntas.

## Estado documentado

Disponibles: descripción del proyecto, requerimientos identificados con criterios de aceptación y reglas, selección actualizada de tendencias, imagen, matriz completada y [primera iteración del ADD](05_ADD_ITERACION_1.md) consolidada a nivel de diseño. La estructura del código se encuentra descrita en [Organización del código](ORGANIZACION_DEL_CODIGO.md); todavía no hay funcionalidades implementadas. Pendientes de verificar o producir: validación del caso, modalidad de participación, estimación del alcance según disponibilidad, implementación ejecutable, evidencias reales, conclusiones y diapositivas. Los trabajos de Tarea2 no acreditan por sí solos la implementación del sistema de salud.

El artículo individual tipo paper técnico corresponde como máximo a la semana 16; los lineamientos no lo exigen para la semana 8. Debe abordar una tendencia implementada e incorporar las correcciones indicadas.
