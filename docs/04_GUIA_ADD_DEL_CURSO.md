# Guía para desarrollar ADD según la clase

Fuente: TEO_02_Atributos_Drivers_y_Metodos.pdf, Hugo R. Cordero, ciclo 2026-II. Archivo proporcionado por el usuario en la carpeta local de fuentes/. Revisión: 6 de octubre de 2026.

## Precisión de terminología

ADD es el método Attribute-Driven Design (diseño guiado por atributos), no el nombre genérico de un documento de arquitectura. Su aplicación produce decisiones, responsabilidades, interfaces y vistas que documentaremos. Los C4 existentes son diseños preliminares que deberán justificarse y refinarse mediante el método.

## Entradas y drivers

Las diapositivas 6 y 7 definen los drivers como el subconjunto de requerimientos que más influye en la estructura del sistema. Deben codificarse, relacionarse con los requerimientos y priorizarse. Se consideran funcionalidad principal, atributos de calidad, restricciones y preocupaciones arquitectónicas.

Tomar como entrada los documentos 01 y 02. La matriz 03 vincula tendencias y requerimientos, pero no sustituye la matriz de elementos y drivers. Agrupar funcionalidad por módulo o usuario y elegir requisitos representativos, evitando convertir cada RF en un driver separado.

Distinguir restricciones del curso (nube, arquitectura permitida y seis tendencias) de decisiones del proyecto (FastAPI, PostgreSQL y monolito modular), que todavía necesitan justificación. La elección confirmada de tendencias condiciona el diseño, pero una marca no sustituye la descripción de la necesidad.

## Escenarios de atributos de calidad

La diapositiva 3 indica seis campos: origen, estímulo, artefacto, entorno, resultado y medida del resultado.

La actividad de la diapositiva 15 pide cuatro escenarios. Adoptaremos esa cantidad como punto de partida para seguir el formato de clase, sin afirmar que los lineamientos del proyecto fijan exactamente ese número para la presentación parcial.

Candidatos para desarrollar: seguridad de acceso por registro, confiabilidad de reservas/pagos concurrentes, rendimiento bajo la carga de RNF-04 y recuperación según RNF-09/RNF-10. Los valores deben conservar trazabilidad con los requerimientos; las metas adicionales se identificarán como propuestas.

## Entregables y secuencia

1. Tabla de drivers: código, descripción, requerimientos fuente, tipo, prioridad y justificación.
2. Cuatro escenarios medibles de atributos de calidad.
3. Diagrama de contexto, con límites del sistema y roles/sistemas externos pertinentes.
4. Objetivo de la primera iteración y selección de elementos a refinar.
5. Matriz elementos × drivers con códigos relacionados.
6. Conceptos de diseño por elemento: patrones, tácticas, frameworks, modelos de dominio, COTS o servicios externos, solo donde correspondan, con justificación.
7. Responsabilidades concretas e interfaces entre los elementos.
8. Vistas coherentes con las decisiones, y evaluación de cobertura y asuntos pendientes de la iteración.

## Correspondencia entre las diapositivas

La diapositiva 10 muestra el proceso completo: revisar entradas; establecer objetivo de iteración; elegir elementos a refinar; seleccionar conceptos; instanciar elementos, asignar responsabilidades y definir interfaces; dibujar vistas y registrar decisiones; analizar el diseño y revisar el objetivo.

La actividad de la diapositiva 15 organiza la entrega en pasos 2 a 6: identificar elementos, cruzarlos con drivers y agregar conceptos, responsabilidades e interfaces. La numeración no coincide exactamente con el esquema general. Usar el cuadro solicitado por el profesor como formato de entrega, incluyendo además el objetivo, las vistas (diapositiva 13) y la revisión de la iteración.

Formato central de la clase (diapositiva 12):

| Elemento | Códigos de drivers arquitectónicos | Conceptos de diseño | Responsabilidades | Interfaces |
|---|---|---|---|---|
| Código y nombre del elemento | Drivers que atiende | Conceptos aplicables y justificación | Qué hace y qué límites tiene | Con quién interactúa, para qué y mediante qué contrato |

Las interfaces incluyen contratos entre módulos o sistemas; no se limitan a pantallas.

## Límites de interpretación

El caso de redes de agua de la diapositiva 14 es un ejemplo de clase. Sus requisitos (Google Cloud, mapas, funcionamiento móvil sin conexión y notificaciones, entre otros) no se trasladan al sistema de salud. Del mismo modo, los ejemplos de Event Sourcing, MongoDB y otros productos no son decisiones obligatorias para nuestro proyecto.

Estado actualizado al 8 de octubre de 2026: primera iteración consolidada en [05_ADD_ITERACION_1.md](05_ADD_ITERACION_1.md), con drivers, cuatro escenarios, matriz de diseño, interfaces, vistas y evaluación de cobertura. La implementación y la validación de los atributos permanecen pendientes.
