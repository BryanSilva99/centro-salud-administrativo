# AGENTS.md — Centro de Salud Administrativo

## 1. Propósito

Este archivo establece las reglas de desarrollo que deben seguir los agentes de IA que trabajen en el repositorio.

El objetivo es construir un sistema mantenible, coherente, legible y técnicamente sólido, evitando la sobreingeniería y la complejidad innecesaria.

**Prioridad: corrección > claridad > simplicidad > mantenibilidad > optimización prematura.**

La arquitectura debe facilitar la evolución del sistema, no convertirse en una obligación de agregar capas o abstracciones sin necesidad.

## 2. Contexto tecnológico

- Backend: Java 21 con Spring Boot.
- Frontend: React con TypeScript.
- Base de datos: PostgreSQL.
- Arquitectura: monolito modular con principios de arquitectura hexagonal.
- Persistencia: Spring Data JPA y JDBC cuando corresponda.
- Migraciones: Flyway.
- Pruebas: JUnit y Vitest.

No cambiar el stack ni introducir frameworks importantes sin autorización expresa.

Antes de implementar cambios, consultar:
- `docs/ORGANIZACION_DEL_CODIGO.md`
- Documentación de requisitos relacionada con la funcionalidad.
- Código existente del módulo afectado.

El código existente es una referencia de convenciones, no una garantía de que cada decisión anterior sea correcta.

## 3. Control de alcance

Implementar únicamente lo solicitado explícitamente en la tarea actual.

No asumir que una funcionalidad pendiente, una fecha de entrega o una descripción arquitectónica autorizan su implementación.

Se permite realizar ajustes técnicos mínimos directamente necesarios para completar correctamente una tarea autorizada.

No realizar refactorizaciones extensas, cambios arquitectónicos globales ni agregar funcionalidades no solicitadas.

Cuando se identifiquen mejoras fuera del alcance, informarlas sin implementarlas.

No iniciar servicios persistentes ni modificar infraestructura de despliegue sin autorización.

No incorporar credenciales, respaldos, datos reales de pacientes ni archivos ajenos al proyecto.

## 4. Principios de diseño

### Simplicidad

Preferir soluciones directas y fáciles de comprender.

- No introducir abstracciones para necesidades hipotéticas.
- No crear interfaces con el único propósito de tener una interfaz.
- No implementar patrones de diseño si no resuelven un problema concreto.
- Evitar clases intermediarias que solamente deleguen llamadas sin aportar una responsabilidad justificada.
- No crear sistemas de configuración complejos para funcionalidades simples.
- No generalizar código que solo tiene un caso de uso conocido.
- Preferir una duplicación pequeña y explícita antes que una abstracción incorrecta.

### Responsabilidad y cohesión

Cada clase, función y módulo debe tener una responsabilidad identificable.

- Mantener juntas las operaciones fuertemente relacionadas.
- Separar responsabilidades cuando existan motivos reales de cambio independientes.
- Evitar clases que concentren responsabilidades no relacionadas.
- No dividir artificialmente operaciones sencillas en múltiples archivos.
- Mantener una dirección clara de dependencias.

### Acoplamiento

Reducir las dependencias innecesarias.

- Evitar dependencias circulares.
- No acceder directamente a implementaciones internas de otros módulos.
- Utilizar contratos explícitos cuando exista una frontera arquitectónica real.
- No introducir dependencias de terceros para resolver problemas triviales.

## 5. Arquitectura backend

Respetar los principios hexagonales establecidos para el proyecto.

### Dominio

- Contiene entidades, objetos de valor, invariantes y reglas de negocio.
- No depende de Spring, JPA, HTTP ni componentes de infraestructura.
- Mantener las reglas próximas a los conceptos que las gobiernan.
- No crear servicios de dominio si la lógica pertenece naturalmente a una entidad.

### Aplicación

- Coordina casos de uso y operaciones de negocio.
- Utiliza puertos para capacidades externas cuando corresponde.
- No contiene detalles de controladores HTTP ni persistencia.
- No crear un caso de uso independiente para cada operación trivial de manera automática.

### Adaptadores

- Traducen entre el exterior y los contratos internos.
- Los controladores no deben contener reglas de negocio.
- Los repositorios no deben decidir políticas de negocio.
- No exponer entidades JPA directamente como respuestas HTTP.

### Módulos

- Mantener límites funcionales claros.
- Evitar dependencias cruzadas innecesarias.
- No implementar módulos futuros por anticipación.
- Las decisiones de diseño deben ser consistentes con los módulos existentes.

## 6. Calidad del código

### Legibilidad

- Utilizar nombres descriptivos y consistentes.
- Preferir código explícito sobre expresiones compactas difíciles de interpretar.
- Evitar abreviaturas ambiguas.
- Mantener funciones enfocadas.
- Evitar niveles excesivos de anidamiento.
- Usar retornos tempranos cuando mejoren la claridad.
- Escribir comentarios que expliquen decisiones o restricciones, no que repitan el código.

### Consistencia

Antes de crear un archivo o componente:
1. Revisar cómo se resuelven responsabilidades equivalentes.
2. Respetar nombres y convenciones establecidos.
3. Reutilizar mecanismos existentes cuando sean adecuados.
4. Evitar crear una segunda forma de resolver el mismo problema.

No copiar implementaciones existentes que contengan problemas evidentes; proponer una corrección coherente cuando corresponda.

### Control de complejidad

Antes de introducir una nueva abstracción, comprobar:

- ¿Qué problema concreto resuelve?
- ¿Reduce realmente la complejidad total?
- ¿Es necesaria ahora?
- ¿Facilita comprender o modificar el sistema?

Si no existe una justificación clara, preferir una solución más sencilla.

No utilizar principios SOLID, DDD o arquitectura hexagonal como justificación automática para multiplicar clases.

## 7. Frontend

- Utilizar componentes React funcionales con TypeScript.
- Mantener separadas las responsabilidades de presentación y acceso a datos.
- Evitar componentes excesivamente grandes o con lógica no relacionada.
- Extraer hooks cuando encapsulen comportamiento reutilizable o suficientemente complejo.
- No crear hooks para operaciones triviales.
- Utilizar el cliente HTTP compartido cuando corresponda.
- Evitar duplicar manejo de errores, autenticación y lógica de solicitudes.
- Mantener los tipos coherentes con los contratos del backend.
- Priorizar interfaces comprensibles, accesibles y predecibles.

No añadir librerías de estado, formularios o componentes sin necesidad demostrable.

## 8. Persistencia y seguridad

- Utilizar migraciones versionadas para cambios de esquema.
- No modificar migraciones aplicadas; crear nuevas migraciones.
- Utilizar consultas parametrizadas.
- Definir correctamente los límites transaccionales.
- Considerar concurrencia e integridad cuando la operación lo requiera.
- No exponer información sensible en respuestas o registros.
- Mantener autenticación y autorización en el servidor.
- No almacenar contraseñas ni secretos en texto plano dentro del repositorio.
- Evitar registrar credenciales, tokens o datos sensibles.

No simplificar mecanismos de seguridad existentes únicamente para reducir líneas de código.

## 9. Pruebas

Las pruebas deben verificar comportamientos y reglas relevantes, no simplemente aumentar su cantidad.

- Agregar pruebas de nuevas reglas de negocio.
- Cubrir errores y casos límite significativos.
- Evitar pruebas acopladas a detalles internos.
- No duplicar escenarios sin una razón concreta.
- Mantener las pruebas legibles.
- No cambiar pruebas únicamente para conseguir que pasen cuando revelan un defecto real.

Ejecutar las pruebas relacionadas con cada cambio cuando el entorno lo permita.

No afirmar que una funcionalidad está verificada si sus pruebas no se ejecutaron.

## 10. Refactorización

Aplicar refactorizaciones pequeñas, justificadas y seguras.

Refactorizar cuando:
- Exista duplicación significativa.
- Una responsabilidad esté mal ubicada.
- El código sea innecesariamente difícil de entender.
- Una abstracción haya dejado de aportar valor.
- El cambio solicitado lo requiera para mantener coherencia.

No refactorizar módulos no relacionados sin autorización.

Preservar el comportamiento observable durante refactorizaciones.

## 11. Flujo obligatorio de trabajo

### Antes de implementar

1. Comprender el requisito solicitado.
2. Inspeccionar el código relacionado.
3. Identificar las convenciones existentes.
4. Determinar el cambio mínimo suficiente.
5. Evaluar riesgos de seguridad, concurrencia o compatibilidad cuando corresponda.

Para cambios complejos, presentar primero una propuesta técnica breve.

### Durante la implementación

- Mantener el alcance controlado.
- Evitar archivos innecesarios.
- Respetar los límites arquitectónicos.
- No introducir complejidad especulativa.
- Incorporar pruebas proporcionales al cambio.

### Antes de finalizar

Revisar:

- ¿Existe una solución más simple?
- ¿Introduje clases o abstracciones prescindibles?
- ¿El código mantiene las convenciones del proyecto?
- ¿Hay duplicación innecesaria?
- ¿Las responsabilidades están correctamente distribuidas?
- ¿Se respetan los límites arquitectónicos?
- ¿El comportamiento existente permanece intacto?
- ¿Las pruebas relevantes pasan?

Corregir los problemas encontrados dentro del alcance autorizado.

## 12. Comunicación

Al finalizar una tarea, informar brevemente:

1. Qué se implementó o modificó.
2. Qué decisiones técnicas relevantes se tomaron.
3. Qué pruebas o comprobaciones se ejecutaron y sus resultados.
4. Qué riesgos o limitaciones permanecen.

Evitar explicaciones extensas de cambios triviales.

No presentar automáticamente nuevas funcionalidades como si fueran necesarias para terminar la tarea actual.

## 13. Principio fundamental

**El mejor código no es el que utiliza más patrones, capas o abstracciones, sino el que resuelve correctamente el problema y puede ser comprendido, probado y modificado con el menor esfuerzo razonable.**

Si dos soluciones son igualmente correctas y respetan los requisitos, preferir la que tenga menor complejidad y mayor claridad.
