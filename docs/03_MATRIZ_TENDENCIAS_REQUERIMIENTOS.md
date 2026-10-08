# Matriz de tendencias vs. requerimientos

Versión 1.0 · 6 de octubre de 2026.

**Estado:** selección confirmada y matriz de diseño completada. La aplicación y las evidencias descritas están pendientes de implementación y verificación.

Base: [Descripción del proyecto](01_CONTEXTO_Y_PROBLEMA.md), [requerimientos](02_REQUERIMIENTOS_DEL_SISTEMA.md) y [evaluación de alternativas](03_EVALUACION_DE_TENDENCIAS.md).

## 1. Selección y cumplimiento de categorías

| ID | Categoría del Radar | Tendencia seleccionada | Entrada / anillo, abril de 2026 |
|---|---|---|---|
| T-01 | Techniques | Zero trust architecture | 6 / Adopt |
| T-02 | Techniques | Mutation testing | 11 / Trial |
| T-03 | Tools | OpenAI Codex | 75 / Trial |
| T-04 | Tools | Axe-core | 66 / Adopt |
| T-05 | Platforms | SigNoz | 49 / Trial |
| T-06 | Languages and Frameworks | React JS | 100 / Adopt |

Se cumplen dos técnicas, dos herramientas, una plataforma y un lenguaje/framework. El grupo denominado “Technologies” en los lineamientos se interpreta como el cuadrante oficial Techniques. Los nombres y números se verificaron en las páginas impresas 12 y 13 del [Radar Vol. 34 oficial](https://www.thoughtworks.com/content/dam/thoughtworks/documents/radar/2026/04/tr_technology_radar_vol_34_en.pdf).

Las instrucciones compartidas y un entorno reproducible siguen siendo prácticas útiles, pero Curated Shared Instructions y Dev Containers ya no ocupan posiciones entre las seis. Claude Code no se cuenta: la herramienta elegida es OpenAI Codex. Una biblioteca auxiliar de pruebas no constituye automáticamente otra tendencia seleccionada.

## 2. Cómo interpretar las relaciones

- **Implementación:** contribuye a construir el comportamiento requerido.
- **Verificación:** comprueba propiedades del producto; no implementa por sí misma la regla.
- **Observación:** permite investigar la ejecución y medirla; no garantiza su resultado.
- **Desarrollo:** apoya la construcción y revisión del código; no satisface automáticamente sus funciones.

La matriz muestra asociaciones concretas, no una relación artificial entre todas las tendencias y todos los requerimientos. Los vínculos de apoyo o verificación no equivalen a cobertura funcional completa.

## 3. Matriz principal

| Tendencia / tipo de aporte | Requerimientos y reglas relacionados | Aplicación prevista | Evidencia y criterio para demostrar el aporte |
|---|---|---|---|
| **T-01 · Zero trust architecture** — Implementación de seguridad | RF-01, RF-02: sesiones y cuentas; RF-12, RF-17: agenda y atención propias; RF-23: autorización de anulaciones. RN-01, RN-02, RN-14. RNF-01, RNF-02. | Verificar identidad, acción y registro en cada operación; aplicar mínimo privilegio y separación solicitante/aprobador. Definir credenciales independientes para aplicación y migraciones y limitar acceso a base de datos y observabilidad. | **EV-01:** ejecutar casos permitidos y denegados de la matriz de roles, cambio de identificadores sin acceso indebido, cuenta desactivada y autoaprobación rechazada. Adjuntar decisiones de confianza y permisos técnicos. Un login aislado no basta. |
| **T-02 · Mutation testing** — Verificación de reglas | RF-13: reserva; RF-14: reprogramación; RF-21: pago; RF-23: reversión; RF-24: cierre. RN-07, RN-13, RN-14, RN-15. RNF-12; apoyo a RNF-03 y RNF-07. | Introducir cambios controlados en una copia del código del dominio para comprobar la capacidad de las pruebas de detectar errores en superposición, importes, aprobación y cierre. | **EV-02:** suite original correcta, informe de mutaciones detectadas y supervivientes y análisis de cada superviviente relevante. Las alteraciones que cambian reglas deben ser detectadas por pruebas; justificar equivalentes y exclusiones. Mantener pruebas independientes de concurrencia. |
| **T-03 · OpenAI Codex** — Desarrollo | RNF-07 y exigencia de desarrollo asistido del curso. Tareas de referencia: RF-13, RF-21 o RF-23; solo se atribuirán las efectivamente trabajadas. | Elaborar y revisar implementación y pruebas a partir de requerimientos identificados; ejecutar comprobaciones y revisar decisiones y cambios. | **EV-03:** registrar al menos dos tareas de código ligadas a RF, entrada de trabajo, cambios, revisión y resultados. La documentación asistida existente no sustituye esta evidencia. No afirmar productividad ni corrección sin mediciones. |
| **T-04 · Axe-core** — Verificación de accesibilidad | RNF-08. Pantallas de RF-04/RF-05: pacientes; RF-13/RF-14: citas; RF-21/RF-24: caja. | Ejecutar comprobaciones automáticas de accesibilidad sobre las pantallas y estados definidos; corregir hallazgos y combinar el análisis con recorrido manual por teclado. | **EV-04:** informe reproducible con versiones y estados revisados, sin hallazgos críticos o serios sin resolver; documentar los demás e investigar resultados indeterminados. Verificar teclado y foco manualmente. Cero hallazgos automáticos no equivale a accesibilidad completa. |
| **T-05 · SigNoz** — Observación | RNF-05 directamente; medición y diagnóstico de RNF-04 y RNF-09. Flujos RF-13 y RF-21. | Correlacionar trazas, logs y métricas de reserva y pago y sus operaciones de persistencia; diferenciar rechazo de negocio de error inesperado; excluir datos personales innecesarios. | **EV-05:** seguir una reserva y un pago exitosos y un fallo controlado; localizar duración y componente afectado. Comprobar ausencia de datos excluidos y continuidad del negocio ante caída de telemetría. No usarlo como sustituto de RF-28. |
| **T-06 · React JS** — Implementación de interfaz | RF-04, RF-05, RF-12 a RF-17, RF-19 a RF-25, RF-27, RF-28 y RNF-08. | Construir formularios, agendas, listas de espera, caja, reportes y vistas autorizadas. Gestionar carga, errores y confirmación real de operaciones por la API. | **EV-06:** recorrer los flujos desde la interfaz hasta persistencia, recargar y verificar estado; representar rechazo por conflicto o permisos sin presentar éxito falso. Probar navegación por teclado. Las reglas de negocio también se validan en servidor. |

## 4. Escenarios concretos de demostración

### EV-01. Protección de operaciones y registros

1. Una cuenta PRO consulta su agenda; se rechaza acceder a la de otro profesional alterando el identificador.
2. REC registra una cita, pero no puede aprobar una anulación económica.
3. Una cuenta con CAJ y ADM solicita una reversión y no logra aprobarla por sí misma; otro administrador sí puede resolverla.
4. Desactivar una cuenta impide la siguiente operación aunque conserve una sesión anterior.
5. Revisar límites de acceso entre web, API, base de datos y observabilidad en el ADD y configuración. El aislamiento de red no sustituye identidad ni permisos.

**Guardar:** resultados de pruebas, configuración sin secretos y vista de límites de confianza. **Responsabilidad:** backend y despliegue.

### EV-02. Calidad de pruebas sobre reglas del dominio

Elegir funciones pequeñas de superposición de intervalos, validación de pago, separación de aprobadores y cálculo de efectivo. Ejecutar primero sus pruebas sin mutaciones. Después revisar alteraciones como cambiar un operador de comparación o una resta por suma.

Documentar mutaciones generadas, detectadas, supervivientes, equivalentes, excluidas y no ejecutables. Para una superviviente que altera el comportamiento, mejorar la prueba y repetir la comprobación. Si no hay supervivientes relevantes, presentar ese resultado sin fabricar defectos.

El candidato inicial de ejecución es mutmut para Python; su compatibilidad se verificará al configurar el entorno. La selección del Radar corresponde a Mutation testing, no a mutmut. No se cambiará el backend para utilizar cargo-mutants.

**Guardar:** informe, versión de código, pruebas y explicación de resultados. **Responsabilidad:** dominio y pruebas. **Límite:** las mutaciones no demuestran por sí solas integridad transaccional ni ausencia de carreras.

### EV-03. Uso real del asistente de desarrollo

Registrar dos tareas de implementación concretas, por ejemplo validación de reservas y pruebas de pagos. Para cada una, conservar RF relacionado, instrucciones relevantes, cambios producidos, revisión y verificaciones. Registrar correcciones cuando existan; no simular intervenciones ni resultados.

**Guardar:** historial de cambios y ficha de trabajo sin secretos. **Responsabilidad:** quien implemente y revise. El rol de desarrollo no es un nuevo actor del sistema del centro de salud.

### EV-04. Accesibilidad en procesos internos

Comprobar registro de paciente, reserva/reprogramación y pago/cierre en estos estados: inicial, errores de validación, diálogo de confirmación cuando exista y resultado. Axe-core detectará las reglas automatizables; se complementará con teclado, orden del foco, apertura y cierre de diálogos y comprensión de mensajes.

**Guardar:** reporte por pantalla/estado y lista de revisión manual. **Responsabilidad:** interfaz y pruebas. Las comprobaciones deben repetirse tras corregir hallazgos y quedar ejecutables dentro del proceso de verificación.

### EV-05. Diagnóstico del sistema

Registrar una reserva y un pago con identificador de correlación. Seguir el recorrido en SigNoz y observar duración de API y persistencia cuando estén instrumentadas. Provocar un fallo controlado en un entorno de prueba y explicar su ubicación, sin utilizar información real de pacientes. Interrumpir observabilidad y comprobar que el sistema sigue respondiendo.

**Guardar:** capturas y configuración de instrumentación sin datos personales, junto a resultados de continuidad. **Responsabilidad:** backend y despliegue. SigNoz no convierte automáticamente un sistema lento en rápido; aporta evidencia para diagnosticarlo.

### EV-06. Flujo administrativo y económico

Desde React: registrar paciente, reservar, reprogramar, registrar llegada, iniciar/finalizar atención, registrar cargo y pagos y cerrar caja. Verificar también una atención sin cargo, la presentación de un conflicto y las vistas según permisos. Comprobar los datos después de recargar.

**Guardar:** guion de ejecución y resultados de pruebas de interfaz/integración. **Responsabilidad:** interfaz y backend. El paciente no necesita una cuenta ni portal propio.

## 5. Relación inversa: necesidades y aportes

| Necesidad / referencias | Aportes principales | Trabajo que sigue siendo necesario |
|---|---|---|
| Identidad y permisos: RF-01 a RF-03, RNF-01/RNF-02 | T-01 implementa controles; T-06 presenta acceso; T-03 apoya desarrollo | Elegir autenticación y diseñar sesiones, credenciales y autorización en el ADD. |
| Pacientes: RF-04 a RF-06 | T-06 interfaz; T-04 comprueba accesibilidad; T-01 limita acceso | Unicidad, consistencia y persistencia son responsabilidad de la aplicación/base de datos. |
| Servicios y recursos: RF-07 a RF-11 | T-06 interfaz; T-01 control de configuración | Implementar disponibilidad, bloqueos y resolución de citas afectadas. |
| Agenda y atención: RF-12 a RF-18 | T-06 interfaz; T-01 alcance por profesional; T-02 verifica reglas; T-05 observa reservas | Transacciones, estados y pruebas de concurrencia propias. |
| Caja: RF-19 a RF-24 | T-06 interfaz; T-01 autorizaciones; T-02 verifica cálculos y reglas; T-04 accesibilidad; T-05 observa pagos | Integridad decimal, idempotencia, separación de cuentas y cierres consistentes. |
| Reportes: RF-25 a RF-27 y RF-29 | T-06 presentación; T-01 acceso | Consultas, definiciones de indicadores y reconciliación con datos. SigNoz no produce el reporte administrativo. |
| Auditoría: RF-28 | T-01 limita acceso; T-06 permite consulta | Registro transaccional y protección de eventos de negocio independientes de la telemetría. |
| Operación: RNF-04/RNF-05/RNF-09 | T-05 observación; T-01 protección | Pruebas de carga, recuperación e infraestructura. |
| Reproducibilidad y recuperación: RNF-06/RNF-10 | Sin atribución forzada a una tendencia | Configuración de entorno, migraciones, respaldos y ensayo de restauración. |
| Desarrollo y verificación: RNF-07/RNF-08/RNF-12 | T-03 proceso; T-02 calidad de pruebas; T-04 accesibilidad | Revisión humana, pruebas funcionales y revisiones manuales. |

## 6. Seguimiento de implementación

| Evidencia | Diseño definido | Implementación | Verificación | Entrega prevista |
|---|---|---|---|---|
| EV-01 / T-01 | Sí | Pendiente | Pendiente | P8 |
| EV-02 / T-02 | Sí | Pendiente | Pendiente | P8 |
| EV-03 / T-03 | Sí | Pendiente para tareas de código | Pendiente | P8 |
| EV-04 / T-04 | Sí | Pendiente | Pendiente | P8 |
| EV-05 / T-05 | Sí | Pendiente | Pendiente | P8 |
| EV-06 / T-06 | Sí | Pendiente | Pendiente | P8 |

Para cerrar una fila, se adjuntarán la versión de código evaluada, entorno, procedimiento, resultados y limitaciones. No basta con una captura de instalación. Para F16 se ampliarán las verificaciones a las funciones adicionales y se incorporarán correcciones de la revisión parcial.

Las seis tendencias deben aportar evidencia en P8, conforme a la meta del proyecto. No se atribuye esta distribución concreta de pruebas a una exigencia textual de la rúbrica.

## 7. Referencias y decisiones pendientes de arquitectura

La [evaluación](03_EVALUACION_DE_TENDENCIAS.md) conserva las alternativas comparadas. Fuentes de detalle: [Mutation testing](https://www.thoughtworks.com/radar/techniques/mutation-testing), [Axe-core](https://github.com/dequelabs/axe-core), [mutmut](https://mutmut.readthedocs.io/en/latest/) y [SigNoz](https://www.thoughtworks.com/radar/platforms/signoz).

El ADD deberá concretar autenticación, límites de confianza, persistencia transaccional, instrumentación, estructura de componentes React y entorno de ejecución de pruebas. Estas decisiones no requieren nuevas funcionalidades de negocio para justificar las tendencias seleccionadas.
