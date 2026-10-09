# Evaluación de tendencias frente a los requerimientos

Fecha: 6 de octubre de 2026.

Estado: selección aceptada por el usuario. La matriz resultante está en [03_MATRIZ_TENDENCIAS_REQUERIMIENTOS.md](03_MATRIZ_TENDENCIAS_REQUERIMIENTOS.md). Se conserva el análisis de alternativas como antecedente. Aplicación y evidencias pendientes de implementación.

## 1. Criterio de evaluación

Se revisó el Technology Radar Vol. 34, de abril de 2026, tanto en la traducción local como en la edición oficial inglesa indicada por los lineamientos. La selección debe respetar dos técnicas, dos herramientas, una plataforma y un lenguaje/framework.

El primer grupo aparece como “Technologies” en los lineamientos, pero el cuadrante oficial del Radar se denomina “Techniques”. Se mantiene la interpretación usada en el proyecto: dos técnicas. No existe un quinto cuadrante llamado Technologies en esta edición.

Se evalúa cada opción según:

1. Necesidad existente que atiende, evitando agregar funciones solo para justificarla.
2. Relación con requerimientos funcionales, calidad o proceso de desarrollo.
3. Evidencia concreta que podrá mostrarse en la presentación.
4. Complejidad razonable para React, Spring Boot y PostgreSQL y para trabajo individual o grupal.
5. Aporte distinto de las otras tendencias seleccionadas.

Una tendencia no necesita generar una pantalla para ser útil. Las pruebas, seguridad y observabilidad tienen valor verificable. Tampoco basta instalar una herramienta o nombrarla en un requisito para demostrar aporte.

## 2. Revisión de la selección anterior

| Selección anterior | Necesidad real | Evaluación y recomendación |
|---|---|---|
| Zero Trust Architecture | Control por rol y por registro; protección de datos y de operaciones económicas | Mantener. Se debe aplicar a límites de confianza y credenciales, no reducirla a login y botones ocultos. |
| Curated Shared Instructions | Consistencia del desarrollo asistido y mantenimiento de reglas arquitectónicas | Útil, pero su relación con resultados funcionales es indirecta. Un archivo de instrucciones aislado sería evidencia débil de una práctica curada. Se acordó sustituirla entre las seis por Mutation testing, conservando instrucciones de desarrollo como práctica de apoyo. |
| Claude Code / Codex | Desarrollo con el asistente que realmente utiliza el responsable | Mantener Codex. Para el conteo y la matriz usar el nombre oficial OpenAI Codex; Claude Code puede seguir como alternativa de trabajo sin contabilizar una séptima tendencia ni atribuirle uso. |
| Dev Containers | Entorno reproducible y reducción de diferencias entre máquinas | Es una elección válida incluso trabajando solo. Se acordó sustituirla entre las seis por Axe-core, por su relación más directa con la accesibilidad ya requerida. Esto no obliga a retirar contenedores del entorno. |
| SigNoz | Localizar errores, latencia y comportamiento de operaciones de reserva y caja | Mantener. Su alcance es observabilidad técnica; no sustituye la auditoría del negocio ni el dashboard administrativo. |
| React JS | Interfaz interna con formularios, agendas, estados y reportes | Mantener. Aporta al producto sin incorporar procesos adicionales. No garantiza por sí solo accesibilidad, permisos ni rendimiento. |

## 3. Selección adoptada y categoría exacta

Los números y anillos corresponden a la edición de abril de 2026, páginas impresas 12 y 13 del PDF oficial [1].

| Cupo | Categoría oficial | Tendencia | Número | Anillo |
|---|---|---|---|---|
| 1 | Techniques | Zero trust architecture | 6 | Adopt |
| 2 | Techniques | Mutation testing | 11 | Trial |
| 3 | Tools | OpenAI Codex | 75 | Trial |
| 4 | Tools | Axe-core | 66 | Adopt |
| 5 | Platforms | SigNoz | 49 | Trial |
| 6 | Languages and Frameworks | React JS | 100 | Adopt |

Resultado: 2 + 2 + 1 + 1. Las herramientas auxiliares utilizadas para aplicar una técnica no ocupan automáticamente otra posición de esta lista.

## 4. Aplicación y evidencia previstas

### 4.1 Zero trust architecture: acceso y límites de confianza

**Relación:** RF-01, RF-02, RF-12, RF-17, RF-23; RN-01, RN-02 y RN-14; RNF-01 y RNF-02.

Diseñar autorización de cada solicitud por identidad, acción y registro. Una cuenta profesional no accede a atenciones ajenas. Una cuenta con varios roles no aprueba su propia reversión. Las conexiones internas no reciben confianza automática: las credenciales de la aplicación y las de migración se separan y usan privilegios limitados. El ADD deberá describir sesiones, HTTPS, credenciales y acceso a base de datos y observabilidad.

**Evidencia:** solicitudes permitidas y denegadas, modificación de identificadores sin acceso indebido, desactivación que invalida acceso y revisión de permisos de las cuentas técnicas. No afirmar una arquitectura Zero Trust completa basándose solo en roles de pantalla.

### 4.2 Mutation testing: comprobar si las pruebas detectan errores del negocio

**Relación:** RF-13, RF-14, RF-21, RF-23 y RF-24; RN-07, RN-13, RN-14 y RN-15. Refuerza la verificación de RNF-03; no implementa por sí misma la integridad.

La técnica introduce alteraciones pequeñas en una copia del código y ejecuta las pruebas para comprobar que detecten el cambio [2]. Aplicarla primero a funciones del dominio: superposición de horarios, cálculo de saldo, autorización de reversión y cálculo del cierre.

**Ejemplos previstos:** cambiar la comparación de importe contra saldo; alterar la resta de devoluciones; invertir la condición que impide aprobar una solicitud propia. Una prueba que no detecte un cambio de comportamiento requiere investigación y posiblemente mejora.

**Evidencia:** código y pruebas de referencia, informe de mutaciones detectadas y supervivientes, análisis de los casos restantes y mejora de una prueba insuficiente. No exigir un porcentaje universal sin revisar mutaciones equivalentes, exclusiones y errores de ejecución. No reemplaza las pruebas de concurrencia ni las transacciones de base de datos.

**Viabilidad:** PIT es el candidato para pruebas de mutación del dominio Java [4]. Su integración y compatibilidad con el entorno de pruebas se verificarán cuando se autorice implementar. La tendencia seleccionada es Mutation testing; PIT es una herramienta auxiliar y no ocupa una séptima posición en el Radar. Limitar inicialmente las mutaciones al dominio, evitando ruido de infraestructura.

### 4.3 OpenAI Codex: desarrollo asistido verificable

**Relación:** RNF-07 y exigencia del curso de codificar utilizando un asistente de IA. Las tareas asociadas a RF específicos sirven para trazar su uso, no para afirmar que Codex satisface automáticamente esos RF.

Utilizarlo en implementación, pruebas y revisión de tareas concretas, manteniendo comprensión y revisión de los cambios.

**Evidencia:** tarea ligada a un RF, cambios producidos, correcciones revisadas y verificaciones ejecutadas. No atribuir reducción de tiempo o mejora de calidad sin medición. El uso en documentación hasta ahora no demuestra implementación funcional.

### 4.4 Axe-core: accesibilidad de la interfaz

**Relación:** RNF-08, aplicado a RF-04/RF-05 (pacientes), RF-13/RF-14 (citas) y RF-21/RF-24 (caja).

Incorporar comprobaciones automáticas en pantallas y estados representativos: formulario vacío, errores de validación, diálogo de confirmación y resultado de la operación. Revisar etiquetas, nombres accesibles, reglas de contraste y estructura detectable por el motor.

**Evidencia:** hallazgo real durante desarrollo, corrección y comprobación posterior; ejecución automática reproducible. Si no aparecen hallazgos, conservar el informe de las pantallas y estados probados sin inventar defectos.

**Límite:** Axe-core no cubre toda la accesibilidad ni garantiza usabilidad [3, 5]. El recorrido completo con teclado, foco visible y manejo de diálogos seguirá verificándose manualmente. No afirmar cumplimiento completo de WCAG por obtener cero errores automáticos.

### 4.5 SigNoz: observar reservas, pagos y fallos

**Relación:** RNF-05 y diagnóstico de RNF-04/RNF-09; instrumentación de RF-13 y RF-21.

Instrumentar backend y acceso a persistencia para correlacionar una operación con logs, métricas y trazas. Medir latencia y errores sin enviar datos personales innecesarios [6].

**Evidencia:** seguir una reserva o pago, localizar un error controlado y distinguir validación de negocio de fallo inesperado. Observar dónde se consume tiempo. La caída de observabilidad no debe bloquear el negocio.

**Costo:** implica instrumentación y recursos de ejecución adicionales. Mantener separado su consumo del entorno de referencia de rendimiento de API/base de datos. No presentar instalar SigNoz como prueba de observabilidad ni confundir trazas técnicas con RN-17, auditoría del negocio.

### 4.6 React JS: interfaz operativa

**Relación:** RF-04, RF-12 a RF-17, RF-19 a RF-25 y RNF-08, entre otros flujos con interfaz.

Construir formularios y componentes reutilizables para pacientes, agenda, admisión y caja; mostrar carga, errores y confirmaciones consistentes con la respuesta de la API.

**Evidencia:** procesos completos desde la interfaz hasta persistencia. Una reserva o un pago no se presentan como confirmados hasta obtener resultado válido del servidor. React contribuye a la interfaz; las restricciones de dinero y disponibilidad permanecen también en el backend.

## 5. Alternativas consideradas

| Categoría | Alternativa del Radar | Encaje y motivo de no priorizarla |
|---|---|---|
| Techniques | Browser-based component testing (8, Trial) | Buena alternativa para probar formularios y diálogos React en un navegador real [7]. Se prefiere Mutation testing por el peso de las reglas de reservas y caja; las pruebas de interfaz pueden mantenerse como práctica de apoyo. |
| Techniques | Passkeys (4, Adopt) | Aporta a autenticación, pero cambia registro de credenciales, recuperación y parte de RF-01/RF-03. No se añade esa complejidad sin una necesidad de autenticación más específica. |
| Techniques | Semantic layer (14, Trial) | Potencialmente útil para indicadores; el volumen de reportes actual no justifica introducir una capa adicional como una de las seis prioridades. |
| Tools | Dev Containers (73, Trial) | Alternativa válida si se prioriza reproducibilidad sobre accesibilidad automatizada. Puede mantenerse en el proyecto sin ocupar una de las seis posiciones. |
| Tools | cargo-mutants (71, Trial) | No corresponde al backend Java. No se cambia el lenguaje del negocio para acomodar una herramienta de pruebas. |
| Tools | Figma Make (74, Trial) | Útil para prototipos; aporta menos evidencia sobre el comportamiento de la aplicación terminada que Axe-core en este alcance. |
| Platforms | Apache APISIX (43, Trial) | Gateway útil para políticas de tráfico y acceso [8]. Con una API y sin integraciones externas previstas, añade una pieza cuya necesidad es menor que observar las operaciones. No sustituye permisos por paciente, cita o pago. |
| Platforms | ClickStack (51, Assess) | Alternativa de observabilidad; no se identifica una necesidad diferencial que justifique cambiar SigNoz. |
| Languages and Frameworks | Svelte (102, Adopt) | Alternativa viable para la interfaz, pero no hay una carencia de React que justifique cambiar la decisión existente. |
| Languages and Frameworks | React Native (101, Adopt) | Orientado a otro tipo de cliente; el alcance vigente es una aplicación web interna. |

Estas comparaciones son juicios de adecuación al proyecto, no una clasificación general de superioridad.

## 6. Ajustes incorporados al cerrar la matriz

Se neutralizó la redacción de RNF-05 y RNF-06: expresan observabilidad y entorno reproducible sin exigir una marca para justificar retrospectivamente su selección. Las tecnologías concretas se asignan después en la matriz y en el ADD. RNF-07 mantiene instrucciones de desarrollo y uso verificable del asistente porque siguen siendo prácticas útiles aunque no todas ocupen una posición entre las seis.

Acciones incorporadas tras adoptar la selección:

- Incorporados en RNF-12 los criterios de calidad de pruebas para el dominio y la evidencia de mutación; no limitarse a agregar el nombre de una herramienta.
- Precisado en RNF-08 qué pantallas y estados pasan por Axe-core y qué revisión manual complementa el análisis.
- Actualizados selección, matriz, imagen y documentos derivados.
- Se mantiene lenguaje de aplicación prevista hasta contar con evidencia.

## 7. Fuentes

1. [Thoughtworks, Technology Radar Vol. 34, abril de 2026, PDF oficial](https://www.thoughtworks.com/content/dam/thoughtworks/documents/radar/2026/04/tr_technology_radar_vol_34_en.pdf). Categorías, números y anillos: páginas impresas 12 y 13.
2. [Thoughtworks: Mutation testing](https://www.thoughtworks.com/radar/techniques/mutation-testing).
3. [Thoughtworks: Axe-core](https://www.thoughtworks.com/radar/tools/axe-core).
4. [Documentación de PIT](https://pitest.org/).
5. [Repositorio oficial de Axe-core: alcance y revisión manual](https://github.com/dequelabs/axe-core).
6. [Thoughtworks: SigNoz](https://www.thoughtworks.com/radar/platforms/signoz).
7. [Thoughtworks: Browser-based component testing](https://www.thoughtworks.com/radar/techniques/browser-based-component-testing).
8. [Thoughtworks: Apache APISIX](https://www.thoughtworks.com/radar/platforms/apache-apisix).

Consulta de fuentes: 6 de octubre de 2026. Las decisiones de esta evaluación no constituyen resultados de implementación.
