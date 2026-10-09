# Decisiones vigentes del proyecto

Actualización: 8 de octubre de 2026. Selección acordada y primera iteración ADD consolidada; implementación pendiente.

## Alcance y roles

Sistema interno administrativo y operativo de una sede: pacientes, personal, servicios, consultorios, citas, admisión, caja, reportes y auditoría. Dos grupos de usuarios y cuatro roles: recepción/admisión, caja, administrador del centro y profesional de salud. El paciente es entidad del negocio sin acceso directo. El contexto completo está en [01_CONTEXTO_Y_PROBLEMA.md](01_CONTEXTO_Y_PROBLEMA.md).

## Seis tendencias confirmadas

| Categoría | Tendencia | Aporte |
|---|---|---|
| Techniques | Zero trust architecture | Identidad, mínimo privilegio y autorización por operación y registro. |
| Techniques | Mutation testing | Verificar que las pruebas detectan errores en reglas del dominio. |
| Tools | OpenAI Codex | Desarrollo asistido con revisión y evidencia de código. |
| Tools | Axe-core | Comprobaciones automáticas de accesibilidad complementadas manualmente. |
| Platforms | SigNoz | Observabilidad de errores, latencia y operaciones. |
| Languages and Frameworks | React JS | Interfaz interna del centro. |

Se respeta 2 + 2 + 1 + 1. El grupo “Technologies” de los lineamientos corresponde en esta selección a Techniques del Radar. La [evaluación](03_EVALUACION_DE_TENDENCIAS.md) explica las alternativas; la [matriz](03_MATRIZ_TENDENCIAS_REQUERIMIENTOS.md) define relaciones y evidencia prevista.

## Cambios y prácticas de apoyo

Mutation testing reemplaza a Curated Shared Instructions; Axe-core reemplaza a Dev Containers entre las seis tendencias. Se pueden conservar instrucciones compartidas y contenedores como apoyo, sin contarlos como tendencias adicionales. OpenAI Codex es el nombre oficial usado en la matriz; Claude Code no se contabiliza ni se atribuye uso.

React reemplazó a Typer en la selección y a Flutter en la interfaz. La CLI administrativa quedó fuera del alcance.

## Arquitectura y evidencias

React → Spring Boot → PostgreSQL. La API envía telemetría a SigNoz. Backend modular con arquitectura hexagonal y diseño nativo para nube; las decisiones, interfaces y escenarios están consolidados en [ADD — Primera iteración](05_ADD_ITERACION_1.md). Las herramientas de pruebas y de desarrollo no son componentes de ejecución del producto.

Las pruebas de mutación no reemplazan las transacciones ni las pruebas de concurrencia. Axe-core no acredita accesibilidad completa. SigNoz no sustituye auditoría de negocio. Codex no acredita cumplimiento de un RF sin implementación revisada y probada.

La primera iteración está cerrada a nivel de diseño, no de validación. La siguiente refina identidad, transacciones y auditoría junto con reserva y admisión. Los contratos HTTP completos, el esquema físico y la configuración de despliegue siguen pendientes según la sección 11 del ADD.

## Entregables y conservación

- Requerimientos: [02_REQUERIMIENTOS_DEL_SISTEMA.md](02_REQUERIMIENTOS_DEL_SISTEMA.md), versión 1.1; RNF-08 ampliado y RNF-12 incorporado.
- Preparación parcial: [PRESENTACION_PARCIAL.md](PRESENTACION_PARCIAL.md).
- Lineamientos oficiales y Technology Radar se conservan íntegros.
- El PDF C4 es local y conserva la vista preliminar; las vistas detalladas vigentes están en el ADD. No se modificó el proyecto remoto de IcePanel.

## Stack confirmado el 8 de octubre de 2026

El usuario eligió Java con Spring Boot para el backend y React para el frontend. Se conservan PostgreSQL, SigNoz y la arquitectura hexagonal de monolito modular. PIT queda como candidato para ejecutar Mutation testing sobre Java; las seis tendencias no cambian. El estado autorizado es solo estructura: no implementar funcionalidades. Los PDF e imágenes anteriores deben actualizarse antes de presentarlos; la documentación Markdown refleja la elección vigente.
