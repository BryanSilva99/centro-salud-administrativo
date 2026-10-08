# Descripción del proyecto: sistema administrativo para un centro de salud

Actualización: 6 de octubre de 2026.

## 1. Descripción general

El proyecto consiste en implementar un sistema web de uso interno para la gestión administrativa y operativa de un centro de salud. Integra la información de pacientes, personal, servicios, consultorios, citas, admisión y caja, y proporciona reportes y trazabilidad de las operaciones.

La aplicación permite coordinar los recursos disponibles, gestionar el recorrido administrativo de las atenciones y controlar los movimientos económicos asociados a los servicios cuando corresponde. Su alcance comprende una sede y atención ambulatoria programada.

Los usuarios directos son el personal administrativo y los profesionales de salud. El paciente es beneficiario del servicio y una entidad central de información; las operaciones relacionadas con él son realizadas por el personal autorizado.

## 2. Contexto y justificación

La operación de un centro de salud requiere coordinar la identificación de pacientes, la oferta de servicios, la disponibilidad de profesionales y consultorios, las atenciones programadas y los registros económicos. La información debe estar disponible para cada responsable, con permisos acordes con sus funciones y trazabilidad de los cambios.

En el contexto peruano, la orientación oficial de Gob.pe contempla canales presenciales, telefónicos y en línea para solicitar citas de consulta externa del Minsa [1]. Por su parte, en agosto de 2022 la Defensoría del Pueblo reportó dificultades de acceso a cupos y de atención telefónica en policlínicos de EsSalud de Cusco, y recomendó fortalecer la gestión de citas y evaluar las brechas de personal [2]. Estos antecedentes muestran la relevancia de la coordinación administrativa; corresponden a sus respectivos ámbitos y fechas.

El proyecto aborda esa necesidad desde la gestión interna del centro: vincular los registros operativos y económicos, facilitar la consulta de información y controlar las operaciones. Su contribución se concentra en la organización y trazabilidad de los procesos que el personal puede gestionar mediante la aplicación.

## 3. Problemática que aborda el sistema

El problema central es la dificultad de coordinar información y operaciones relacionadas cuando los registros de pacientes, recursos, atenciones y movimientos económicos se gestionan de manera separada. La falta de una visión integrada puede generar inconsistencias, trabajo repetido y dificultades para supervisar la jornada.

El sistema se orienta a atender los siguientes problemas de gestión:

| Área | Problema que se busca prevenir o resolver |
|---|---|
| Información de pacientes | Duplicidad de registros y dificultad para consultar antecedentes administrativos. |
| Personal y recursos | Falta de visibilidad conjunta de turnos, ausencias, disponibilidad y bloqueos de consultorios. |
| Servicios | Desvinculación entre la prestación ofrecida, su duración, los profesionales habilitados, los recursos y la tarifa aplicable. |
| Citas y admisión | Reservas incompatibles, cambios descoordinados y dificultad para conocer el estado de las atenciones. |
| Caja | Dificultad para relacionar cargos, pagos, saldos y anulaciones con los servicios y conciliar los movimientos de la jornada. |
| Supervisión | Necesidad de consolidar información de distintas operaciones para obtener indicadores confiables. |
| Control de acceso y auditoría | Dificultad para limitar operaciones sensibles y reconstruir quién realizó un cambio y cuándo. |

## 4. Objetivos

### Objetivo general

Implementar un sistema web que integre la gestión administrativa y operativa de un centro de salud, coordinando pacientes, personal, servicios, recursos, atenciones y caja, con control de acceso, trazabilidad y reportes para apoyar la supervisión del establecimiento.

### Objetivos específicos

1. Centralizar el registro y la consulta de pacientes y su historial administrativo.
2. Gestionar servicios, profesionales, turnos y consultorios para determinar la disponibilidad de atención.
3. Coordinar la programación, reprogramación, cancelación y seguimiento de las atenciones, evitando reservas incompatibles.
4. Registrar cargos y pagos cuando corresponda, controlar saldos y facilitar el cierre de caja.
5. Proporcionar indicadores operativos y económicos consistentes con las operaciones registradas.
6. Aplicar permisos según las responsabilidades de cada usuario y conservar la trazabilidad de las operaciones relevantes.

## 5. Usuarios y roles funcionales

El sistema contempla dos grupos de usuarios directos y cuatro roles funcionales:

| Grupo | Rol | Responsabilidades principales |
|---|---|---|
| Personal administrativo | Recepción / admisión | Registrar y actualizar pacientes; gestionar citas; consultar disponibilidad; registrar llegadas y gestionar la espera. |
| Personal administrativo | Caja | Registrar cargos y pagos según las condiciones del servicio; consultar saldos; realizar cierres y tramitar anulaciones según sus permisos. |
| Personal administrativo | Administrador del centro | Configurar servicios, profesionales, horarios, consultorios, usuarios y permisos; supervisar reportes y autorizar operaciones sensibles. |
| Profesionales de salud | Profesional de salud | Consultar su agenda y los pacientes que han llegado; registrar el inicio y la finalización de sus atenciones. |

Los roles representan responsabilidades, no necesariamente personas o puestos exclusivos. Una misma cuenta puede reunir los roles que correspondan a sus funciones. Pertenecer al grupo administrativo no concede acceso total: cada operación requiere los permisos pertinentes.

El paciente se registra como entidad del negocio y participa en el proceso de atención, pero no dispone de cuenta ni interactúa directamente con la aplicación. No se incluye un portal del paciente en el alcance inicial.

## 6. Alcance funcional

| Módulo | Funcionalidades comprendidas |
|---|---|
| Pacientes | Registro, actualización, búsqueda, control de duplicidad e historial administrativo de citas, asistencias y operaciones económicas. |
| Personal y disponibilidad | Registro de profesionales, servicios habilitados, turnos, ausencias y bloqueos; identificación de citas afectadas por cambios de disponibilidad. |
| Servicios y consultorios | Catálogo de prestaciones, duración estimada, tarifa cuando corresponda, recursos habilitados y bloqueos de consultorios. |
| Citas y admisión | Programación, reprogramación y cancelación; validación de disponibilidad; llegada, espera, inicio, finalización y ausencia. |
| Caja y control económico | Registro de cargos por servicios, pagos y saldos; anulaciones justificadas y cierre con conciliación de movimientos. |
| Reportes y supervisión | Citas y atenciones por periodo, servicio y profesional; ocupación, ausencias, tiempos de espera, ingresos, saldos y cierres de caja. |
| Usuarios, permisos y auditoría | Gestión de cuentas, asignación de roles, autorización de operaciones y registro de cambios relevantes. |

Los servicios podrán contemplar atención con cargo o sin cargo, según la condición administrativa definida. El módulo de caja no implica que toda atención genere un pago del paciente. Las reglas detalladas se establecerán en el documento de requerimientos.

El control de insumos de uso general se mantiene como posible ampliación posterior; no forma parte del núcleo comprometido en este documento.

## 7. Proceso operativo integrado

1. El administrador configura los servicios, sus condiciones y los profesionales y consultorios habilitados; establece horarios y disponibilidad.
2. Recepción identifica al paciente, consulta o actualiza sus datos y registra la solicitud de atención.
3. El sistema permite seleccionar una combinación disponible de servicio, profesional, horario y consultorio, y valida la reserva.
4. Recepción gestiona las cancelaciones y reprogramaciones, con actualización consistente de la disponibilidad.
5. El día de la atención, recepción registra la llegada y el paciente pasa al estado de espera correspondiente.
6. Cuando el servicio genera un cargo, caja registra los movimientos económicos y su estado. La relación temporal entre pago y atención se definirá mediante reglas del negocio, sin establecer aquí el pago como condición universal de atención.
7. El profesional consulta su agenda y registra el inicio y la finalización de la atención. Las ausencias se registran según las reglas del proceso.
8. Los responsables consultan los resultados operativos; caja realiza el cierre de sus movimientos y el administrador revisa reportes y operaciones sensibles.

Los cambios de horario o disponibilidad deben permitir identificar las citas afectadas y gestionar su resolución. Las operaciones relevantes quedan asociadas al usuario que las realizó.

## 8. Límites del sistema

La solución cubre la gestión interna de una sede y requiere conexión para el uso de la aplicación web. Quedan fuera del alcance inicial:

- Portal o aplicación de autoservicio para pacientes.
- Historia clínica electrónica, diagnóstico, prescripción y decisiones clínicas.
- Emergencias, triaje clínico, hospitalización y cirugía.
- Farmacia, dispensación de medicamentos y gestión de recetas.
- Contabilidad integral, facturación electrónica e integración tributaria.
- Verificación de seguros, reembolsos e integración con sistemas oficiales.
- Referencias entre instituciones, operación multisede y funcionamiento sin conexión.

El control de caja comprende el registro y la conciliación de operaciones económicas; no equivale a implementar un sistema contable o de facturación electrónica.

## 9. Resultados esperados y criterios de comprobación

| Resultado esperado | Comprobación prevista |
|---|---|
| Información administrativa consistente | Consultar y actualizar un mismo registro de paciente desde las funciones autorizadas, con controles de duplicidad. |
| Uso coordinado de los recursos | Rechazar reservas incompatibles, incluso ante solicitudes simultáneas. |
| Cambios de agenda consistentes | Reprogramar o cancelar sin duplicar reservas y reflejar la disponibilidad resultante. |
| Visibilidad del estado de atención | Consultar el recorrido desde la llegada hasta la finalización o ausencia. |
| Control económico trazable | Relacionar cargos, pagos, saldos y anulaciones, y verificar los totales del cierre. |
| Supervisión basada en registros | Comprobar que los reportes coinciden con las operaciones realizadas. |
| Acceso controlado | Permitir operaciones autorizadas y rechazar las ajenas a las responsabilidades del usuario. |
| Trazabilidad de cambios | Identificar actor, acción, recurso y momento de las operaciones auditadas. |

La mejora de tiempos y productividad se evaluará mediante mediciones. Se distinguirá el tiempo de espera entre llegada e inicio de atención del intervalo entre solicitud y fecha de la cita.

## 10. Orientación tecnológica

La base tecnológica definida es React para la interfaz web, FastAPI para el backend y PostgreSQL para persistencia, con SigNoz para observabilidad. Se plantea arquitectura hexagonal y diseño nativo para nube.

Las tendencias seleccionadas son Zero Trust Architecture, Mutation testing, OpenAI Codex, Axe-core, SigNoz y React JS. Su aplicación prevista y evidencia se definen en la [matriz de tendencias frente a requerimientos](03_MATRIZ_TENDENCIAS_REQUERIMIENTOS.md). Las decisiones técnicas se desarrollarán en el ADD.

## 11. Referencias

[1] Gob.pe. [Obtener cita médica en un establecimiento del Minsa](https://www.gob.pe/32985-obtener-cita-medicaen-un-establecimiento-del-minsa). Última actualización indicada: 21 de julio de 2023. Consulta: 6 de octubre de 2026.

[2] Defensoría del Pueblo. [Defensoría del Pueblo solicita a la Red Asistencial EsSalud – Cusco fortalecer atención de citas](https://www.defensoria.gob.pe/defensoria-del-pueblo-solicita-a-la-red-asistencial-essalud-cusco-fortalecer-atencion-de-citas/). 1 de agosto de 2022. Consulta: 6 de octubre de 2026.
