# Requerimientos del sistema administrativo para un centro de salud

Versión: 1.1 · Fecha: 6 de octubre de 2026.

Base de alcance: [Descripción del proyecto](01_CONTEXTO_Y_PROBLEMA.md).

## 1. Propósito y alcance

Este documento define las funciones, permisos, reglas del negocio y condiciones de calidad de la aplicación interna del centro de salud. Constituye la base para diseñar la arquitectura, organizar la implementación y verificar los resultados.

Se contempla una sede con atención ambulatoria programada. El sistema integra pacientes, profesionales, servicios, consultorios, disponibilidad, citas, admisión, caja, reportes y auditoría. El paciente es una entidad del negocio y beneficiario del servicio, sin acceso directo a la aplicación.

No se incluyen historia clínica, diagnóstico, recetas, farmacia, emergencias, hospitalización, contabilidad integral, facturación electrónica, verificación de seguros, integraciones oficiales, portal del paciente, funcionamiento sin conexión ni múltiples sedes. El control de insumos permanece como ampliación fuera de esta versión.

### Convenciones y prioridades

- **RF:** requerimiento funcional. Cada criterio CA se identifica por el RF al que pertenece; por ejemplo, RF-01/CA-1.
- **RN:** regla del negocio, aplicable a todos los canales de acceso.
- **RNF:** requisito de calidad u operación.
- **P8:** alcance objetivo propuesto para la presentación parcial de la semana 8.
- **F16:** complemento previsto para la entrega final de la semana 16.

P8 y F16 son prioridades de planificación del proyecto; no son porcentajes ni módulos impuestos por los lineamientos del curso. Ninguna etiqueta acredita implementación. Las reglas y parámetros de esta versión son decisiones de diseño que pueden revisarse manteniendo su trazabilidad.

## 2. Usuarios y permisos

### 2.1 Grupos y roles

| Grupo | Rol | Propósito |
|---|---|---|
| Personal administrativo | Recepción / admisión (REC) | Gestionar pacientes, citas y llegadas. |
| Personal administrativo | Caja (CAJ) | Gestionar cargos, cobros y cierre de movimientos. |
| Personal administrativo | Administrador del centro (ADM) | Configurar recursos y accesos, supervisar y autorizar operaciones sensibles. |
| Profesionales de salud | Profesional de salud (PRO) | Consultar su agenda y registrar sus atenciones. |

Una cuenta puede reunir varios roles. El administrador no recibe automáticamente las funciones de recepción o caja: deberá tener también el rol correspondiente. La autorización de anulaciones económicas exige una cuenta distinta de la solicitante, aunque esta posea varios roles.

### 2.2 Matriz de acceso

“Propio” limita el acceso a recursos asociados al usuario. “Mínimo” restringe los campos a los necesarios para la tarea. Un guion indica ausencia de permiso con ese rol aislado.

| Operación | REC | CAJ | ADM | PRO |
|---|---|---|---|---|
| Crear y actualizar pacientes | Sí | — | — | — |
| Consultar identificación del paciente | Sí | Mínimo para cobro | — | Mínimo de sus citas |
| Consultar historial de citas | Sí | — | — | Sus atenciones |
| Consultar cargos y pagos | — | Sí | Sí | — |
| Gestionar usuarios y asignar roles | — | — | Sí | — |
| Configurar profesionales, servicios, tarifas y consultorios | — | — | Sí | — |
| Configurar disponibilidad y bloqueos | — | — | Sí | — |
| Consultar agendas operativas | Sí | — | Sí | Propia |
| Programar, reprogramar y cancelar citas | Sí | — | — | — |
| Registrar llegada, retiro de espera y ausencia | Sí | — | — | — |
| Consultar cola de espera | Sí | — | Sí | Propia |
| Iniciar y finalizar atención | — | — | — | Propia |
| Registrar cargos y pagos | — | Sí | — | — |
| Abrir y cerrar sesión de caja | — | Propia | — | — |
| Solicitar reversión económica | — | Sí | — | — |
| Autorizar o rechazar reversión | — | — | Sí, si no es solicitante | — |
| Consultar indicadores operativos | Sí | — | Sí | Propios |
| Consultar cierre e indicadores económicos | — | Propios | Todos | — |
| Consultar auditoría global | — | — | Sí | — |

Los identificadores de pacientes en agendas y reportes se limitarán al propósito de la pantalla. El rol ADM no habilita por sí solo el acceso al directorio completo ni la edición de datos personales.

## 3. Requerimientos funcionales

### 3.1 Identidad y acceso

#### RF-01. Iniciar y cerrar sesión · P8

**Actor:** todos los roles. **Reglas:** RN-01, RN-02.

El sistema debe autenticar cuentas activas y permitir terminar su sesión.

- **CA-1:** credenciales válidas de una cuenta activa permiten acceder exclusivamente a las funciones autorizadas.
- **CA-2:** credenciales incorrectas o cuenta inactiva producen una respuesta genérica, sin revelar si la cuenta existe.
- **CA-3:** después de cerrar sesión o vencer su vigencia, reutilizar la sesión en una operación protegida es rechazado.

#### RF-02. Gestionar cuentas y roles · P8

**Actor:** ADM. **Reglas:** RN-01, RN-02, RN-03.

El sistema debe crear, actualizar, activar y desactivar cuentas, asignar los roles definidos y vincular una cuenta profesional a su ficha.

- **CA-1:** el identificador de acceso es único; no se puede vincular una cuenta profesional a dos fichas activas.
- **CA-2:** al retirar un rol o desactivar la cuenta, la siguiente solicitud protegida debe reflejar el cambio, incluso con una sesión previamente abierta.
- **CA-3:** se rechaza desactivar o retirar el rol al último administrador activo.
- **CA-4:** los cambios de acceso registran autor, destinatario, fecha y roles afectados; nunca contraseñas.

#### RF-03. Restablecer acceso y cambiar contraseña · P8

**Actor:** usuario titular; ADM para iniciar restablecimiento. **Reglas:** RN-02.

- **CA-1:** el usuario cambia su contraseña validando la anterior y la política definida en RNF-02.
- **CA-2:** el administrador puede emitir una credencial temporal de un solo uso; el usuario debe establecer una nueva contraseña antes de operar.
- **CA-3:** el restablecimiento invalida las sesiones anteriores y no expone contraseñas existentes.

### 3.2 Pacientes

#### RF-04. Registrar pacientes · P8

**Actor:** REC. **Reglas:** RN-04.

Registrar código interno, tipo y número de documento, nombres, apellidos, fecha de nacimiento y datos opcionales de contacto.

- **CA-1:** se genera un código interno único y se validan campos obligatorios y fechas.
- **CA-2:** se rechaza un segundo registro con el mismo tipo y número de documento, incluso ante solicitudes concurrentes.
- **CA-3:** documentos diferentes permiten homónimos; el nombre no se considera identificador único.

#### RF-05. Buscar y actualizar pacientes · P8

**Actor:** REC; consulta limitada para CAJ y PRO. **Reglas:** RN-01, RN-04, RN-05.

- **CA-1:** recepción busca por documento, nombre o código, con resultados paginados.
- **CA-2:** las actualizaciones conservan el identificador y sus relaciones; un cambio de documento vuelve a verificar unicidad.
- **CA-3:** caja y profesionales reciben únicamente los campos y registros habilitados por la matriz de acceso.
- **CA-4:** si otro usuario modificó el registro desde su lectura, se informa el conflicto sin sobrescribir silenciosamente el cambio.

#### RF-06. Consultar historial administrativo · P8

**Actor:** REC, CAJ, ADM y PRO, según permisos. **Reglas:** RN-01, RN-05.

- **CA-1:** se muestran citas y estados cronológicamente, con filtros de fechas y paginación.
- **CA-2:** el historial económico solo es visible para caja y administración; un profesional solo accede a sus atenciones.
- **CA-3:** no se incluyen campos de diagnóstico, tratamiento ni historia clínica.

### 3.3 Personal, servicios y recursos

#### RF-07. Gestionar profesionales · P8

**Actor:** ADM. **Reglas:** RN-03, RN-06.

Registrar identificación, nombre, especialidad o función, servicios habilitados y estado activo.

- **CA-1:** solo profesionales activos y habilitados para un servicio pueden recibir nuevas reservas de ese servicio.
- **CA-2:** se conservan las referencias históricas al desactivar un profesional.
- **CA-3:** antes de desactivar se listan las citas futuras afectadas y se exige resolverlas según RN-08.

#### RF-08. Gestionar servicios y condiciones económicas · P8

**Actor:** ADM. **Reglas:** RN-06, RN-12.

Definir nombre, duración en minutos, estado, profesionales y consultorios habilitados y modalidades con o sin cargo.

- **CA-1:** la duración debe ser positiva; las tarifas no pueden ser negativas y se expresan en soles con dos decimales.
- **CA-2:** cada modalidad permite determinar el importe aplicable sin consultar seguros externos.
- **CA-3:** cambiar duración o tarifa no modifica automáticamente citas o cargos ya registrados.

#### RF-09. Gestionar consultorios · P8

**Actor:** ADM. **Reglas:** RN-03, RN-06, RN-08.

- **CA-1:** registrar código único, nombre, servicios compatibles y estado habilitado o inhabilitado.
- **CA-2:** un consultorio incompatible o inhabilitado no puede recibir nuevas reservas.
- **CA-3:** se muestran las citas afectadas antes de inhabilitar y se aplica RN-08.

#### RF-10. Configurar turnos y disponibilidad · P8

**Actor:** ADM. **Reglas:** RN-06, RN-07, RN-08.

Definir intervalos por fecha para profesionales y consultorios, incluyendo descansos y periodos no disponibles.

- **CA-1:** la consulta de disponibilidad combina turno, habilitación, duración del servicio, bloqueos y reservas existentes.
- **CA-2:** se rechazan intervalos inválidos y turnos superpuestos del mismo recurso.
- **CA-3:** modificar un turno no deja citas existentes fuera de disponibilidad sin el tratamiento definido en RN-08.

#### RF-11. Gestionar ausencias, bloqueos e impacto · P8

**Actor:** ADM; REC resuelve las citas. **Reglas:** RN-08.

- **CA-1:** al preparar un bloqueo con motivo e intervalo, se obtiene la lista de citas afectadas.
- **CA-2:** el bloqueo pendiente impide nuevas reservas en el intervalo y deja las citas existentes identificadas como pendientes de resolución, sin cancelarlas automáticamente.
- **CA-3:** recepción reprograma o cancela las afectadas; cuando ninguna queda pendiente se activa el bloqueo definitivo.
- **CA-4:** una cita afectada por el bloqueo no puede iniciar atención hasta resolver el conflicto o retirar el bloqueo de forma autorizada.

### 3.4 Citas y admisión

#### RF-12. Consultar agenda y disponibilidad · P8

**Actor:** REC, ADM y PRO. **Reglas:** RN-01, RN-06, RN-07.

- **CA-1:** consultar agenda diaria o semanal por profesional, servicio y consultorio, con estados diferenciados.
- **CA-2:** el profesional accede exclusivamente a su agenda, también al cambiar identificadores en solicitudes a la API.
- **CA-3:** un horario mostrado como libre se valida nuevamente al confirmar la reserva.

#### RF-13. Programar cita · P8

**Actor:** REC. **Reglas:** RN-06, RN-07, RN-09, RN-12.

Registrar paciente, servicio, modalidad económica, profesional, consultorio, fecha y hora.

- **CA-1:** una reserva válida recibe identificador, estado PROGRAMADA y duración e importe acordados para esa cita.
- **CA-2:** se rechazan conflictos de paciente, profesional o consultorio y reservas fuera de disponibilidad.
- **CA-3:** ante dos solicitudes simultáneas incompatibles, solo una se confirma y la otra recibe un conflicto comprensible.

#### RF-14. Reprogramar cita · P8

**Actor:** REC. **Reglas:** RN-07, RN-09, RN-12.

- **CA-1:** se registra el motivo, horario anterior y nuevo; se conserva el identificador de la cita.
- **CA-2:** si el nuevo horario no es válido, la cita original permanece sin cambios.
- **CA-3:** si la operación se confirma, se libera la reserva anterior y se ocupa la nueva de forma atómica.
- **CA-4:** se mantiene paciente, servicio, duración e importe de origen; un cambio de servicio se gestiona como cancelación y nueva cita.

#### RF-15. Cancelar cita · P8

**Actor:** REC. **Reglas:** RN-09, RN-14.

- **CA-1:** se exige motivo y se registra usuario y momento; el horario queda disponible para otras reservas.
- **CA-2:** no se cancela una atención iniciada o finalizada por esta función.
- **CA-3:** si existen cargos o pagos, se señala la gestión económica pendiente sin borrar ni revertir dinero automáticamente.

#### RF-16. Registrar llegada y gestionar espera · P8

**Actor:** REC; consulta para ADM y PRO. **Reglas:** RN-09, RN-10.

- **CA-1:** una cita programada para el día pasa a EN_ESPERA y guarda la hora de llegada una sola vez.
- **CA-2:** la lista muestra horario de cita, llegada, profesional y tiempo transcurrido; se ordena por horario de cita y luego por llegada.
- **CA-3:** repetir la solicitud de llegada no crea una segunda admisión ni reinicia el tiempo de espera.
- **CA-4:** el orden es administrativo y no asigna prioridades clínicas.

#### RF-17. Iniciar y finalizar atención · P8

**Actor:** PRO. **Reglas:** RN-01, RN-09, RN-10.

- **CA-1:** solo el profesional asignado inicia una cita EN_ESPERA sin conflicto pendiente, pasando a EN_ATENCION.
- **CA-2:** finalizar guarda el momento y pasa a ATENDIDA; se rechaza finalizar sin inicio previo.
- **CA-3:** las marcas temporales se generan en el servidor y se conservan frente a reintentos.
- **CA-4:** el saldo económico no bloquea automáticamente la llegada ni el inicio de atención.

#### RF-18. Registrar ausencia o retiro de espera · P8

**Actor:** REC. **Reglas:** RN-09, RN-10.

- **CA-1:** una cita PROGRAMADA puede marcarse AUSENTE únicamente después de terminar su intervalo y si no tiene llegada registrada.
- **CA-2:** si el paciente se retira antes de ser atendido, EN_ESPERA pasa a CANCELADA con motivo de retiro, conservando la llegada.
- **CA-3:** se rechaza marcar ausencia cuando la atención ya comenzó o finalizó.

### 3.5 Caja y control económico

#### RF-19. Registrar cargo por servicio · P8

**Actor:** CAJ. **Reglas:** RN-12, RN-13.

- **CA-1:** generar un cargo vinculado a paciente y cita con importe obtenido de la condición económica conservada en la reserva.
- **CA-2:** solo existe un cargo vigente por cita; un reintento no duplica la deuda.
- **CA-3:** una modalidad sin cargo muestra importe cero y no genera deuda ni pago obligatorio.
- **CA-4:** no se genera un nuevo cargo para una cita cancelada; los cargos anteriores requieren resolución explícita.

#### RF-20. Abrir sesión de caja · P8

**Actor:** CAJ. **Reglas:** RN-15.

- **CA-1:** registrar fondo inicial no negativo, cajero y fecha/hora del servidor.
- **CA-2:** una cuenta no puede tener dos sesiones de caja abiertas; la apertura concurrente preserva esta restricción.
- **CA-3:** el fondo inicial se distingue de los ingresos por servicios.

#### RF-21. Registrar pagos · P8

**Actor:** CAJ. **Reglas:** RN-13, RN-15.

- **CA-1:** con caja propia abierta, registrar pago total o parcial positivo, medio de pago y referencia externa cuando corresponda.
- **CA-2:** se rechaza un importe mayor que el saldo pendiente, incluso si dos cajeros intentan cobrarlo simultáneamente.
- **CA-3:** el pago reduce el saldo, queda vinculado a la sesión y recibe una constancia interna identificable; no se presenta como comprobante tributario.
- **CA-4:** reenviar la misma operación identificada no registra un segundo pago; los medios no efectivos solo registran una operación externa, sin procesar cobros bancarios.

#### RF-22. Consultar saldos y movimientos · P8

**Actor:** CAJ y ADM. **Reglas:** RN-13.

- **CA-1:** consultar por paciente, cita, fecha y estado de saldo, mostrando cargo, pagos y reversiones.
- **CA-2:** se distingue entre pendiente, parcial, pagado y anulado, según los importes vigentes.
- **CA-3:** el saldo coincide con los movimientos y no depende de valores enviados por la interfaz.

#### RF-23. Solicitar y resolver anulaciones económicas · P8

**Actor:** CAJ solicita; ADM distinto autoriza o rechaza. **Reglas:** RN-14, RN-15.

- **CA-1:** se exige operación original y motivo; una solicitud pendiente no altera saldos ni totales.
- **CA-2:** el solicitante no puede aprobar su propia solicitud, aun teniendo el rol ADM.
- **CA-3:** al aprobar, se crea un movimiento compensatorio vinculado al original; nunca se elimina el registro. La aprobación repetida no duplica la reversión.
- **CA-4:** un cargo con pagos vigentes no se anula hasta resolverlos; si se devuelve dinero, se registra la devolución conforme a RN-14.
- **CA-5:** los cierres anteriores permanecen inalterados; los movimientos nuevos quedan en una sesión abierta del cajero solicitante o se rechaza la ejecución hasta disponer de ella.

#### RF-24. Cerrar caja y conciliar · P8

**Actor:** CAJ para su sesión; ADM consulta. **Reglas:** RN-15.

- **CA-1:** mostrar cobros y devoluciones por medio, efectivo esperado y efectivo declarado por el cajero.
- **CA-2:** calcular diferencia y exigir motivo cuando sea distinta de cero; el cierre con diferencia queda identificado para revisión administrativa.
- **CA-3:** confirmar el cierre congela el resumen y bloquea nuevos cobros o devoluciones en esa sesión, incluso ante solicitudes concurrentes.
- **CA-4:** el cierre no permite borrar pagos ni modificar el fondo inicial; las correcciones se registran en nuevas operaciones.

### 3.6 Supervisión y trazabilidad

#### RF-25. Consultar dashboard operativo · P8

**Actor:** REC, ADM y PRO según alcance. **Reglas:** RN-01, RN-16.

- **CA-1:** mostrar citas del día por estado, pacientes en espera, atenciones finalizadas y ausencias; indicar filtros y momento de actualización.
- **CA-2:** al actualizar después de una operación confirmada, los totales coinciden con los registros correspondientes.
- **CA-3:** el profesional ve solo sus indicadores; recepción no recibe importes económicos.

#### RF-26. Consultar reportes operativos detallados · F16

**Actor:** ADM y PRO para sus indicadores. **Reglas:** RN-16.

- **CA-1:** filtrar por periodo, profesional y servicio; presentar atenciones, ausencias, espera promedio y ocupación programada de consultorios.
- **CA-2:** informar denominadores, unidades y registros sin datos suficientes; no mostrar cero como sustituto de un promedio inexistente.
- **CA-3:** los resultados se reconcilian con las citas y sus marcas temporales sin multiplicar registros por pagos asociados.

#### RF-27. Consultar reportes económicos · P8

**Actor:** ADM; CAJ para sus sesiones y cierres. **Reglas:** RN-13, RN-15, RN-16.

- **CA-1:** mostrar cobros, devoluciones e ingresos netos por periodo y medio; distinguirlos del valor de los cargos y de los saldos pendientes.
- **CA-2:** los periodos usan la fecha del movimiento; una devolución posterior no reescribe los ingresos de un cierre anterior.
- **CA-3:** permitir consultar diferencias de caja y detalle de un cierre autorizado.

#### RF-28. Registrar y consultar auditoría · P8

**Actor:** registro automático; consulta ADM. **Reglas:** RN-17.

- **CA-1:** registrar cambios de usuarios, pacientes, catálogos, disponibilidad, citas, atención y movimientos económicos con actor, acción, recurso, momento y resultado.
- **CA-2:** filtrar por fecha, actor, acción y recurso; correlacionar operaciones relacionadas sin exponer secretos ni copiar datos personales completos.
- **CA-3:** ninguna función ordinaria permite editar o borrar auditoría; una escritura de negocio y su evento obligatorio se confirman juntos o ninguno se confirma.

#### RF-29. Exportar reportes autorizados · F16

**Actor:** ADM; CAJ para sus cierres. **Reglas:** RN-01, RN-16, RN-17.

- **CA-1:** exportar en CSV la información autorizada con los filtros aplicados, títulos y fecha de generación.
- **CA-2:** la exportación respeta los mismos permisos que la pantalla y neutraliza valores interpretables como fórmulas por hojas de cálculo.
- **CA-3:** registrar la exportación en auditoría sin duplicar su contenido sensible en el evento.

## 4. Reglas del negocio

Estas reglas indican qué condiciones deben respetarse al utilizar el sistema. Cada regla incluye una explicación y un ejemplo. Las indicaciones técnicas se presentan por separado para distinguir el comportamiento esperado de su implementación. Se conservan los identificadores utilizados por los requerimientos funcionales.

### RN-01. Cada usuario solo puede realizar las acciones para las que tiene permiso

El sistema debe impedir cualquier acción que no esté autorizada para los roles asignados al usuario. Tener una cuenta no concede acceso a todas las funciones ni a todos los registros.

Si una persona tiene varios roles, podrá realizar las acciones permitidas por esos roles. Aun así, deberá respetar las limitaciones de cada función: un profesional solo puede gestionar sus propias atenciones y quien solicita una anulación económica no puede aprobar esa misma solicitud, aunque también tenga el rol de administrador.

**Ejemplo:** una recepcionista puede registrar pacientes y programar citas, pero no aprobar una devolución. Si también tiene el rol de caja, podrá registrar pagos; eso no le permite autorizar sus propias solicitudes de anulación.

**Indicación técnica:** el servidor debe verificar los permisos cada vez que recibe una solicitud. No basta con ocultar una opción en la pantalla, porque una persona podría intentar ejecutar la operación por otra vía.

### RN-02. Solo las cuentas activas y las sesiones vigentes pueden utilizar el sistema

Un usuario puede operar mientras su cuenta esté activa y su sesión no haya terminado. La sesión finaliza después de 30 minutos sin actividad o al cumplirse 8 horas desde el inicio de sesión, aunque haya seguido utilizando la aplicación.

Al cerrar sesión, desactivar la cuenta o restablecer su contraseña, el acceso anterior deja de ser válido. Para volver a operar se requiere iniciar sesión con una cuenta habilitada.

**Ejemplo:** si el administrador desactiva la cuenta de una persona que dejó de trabajar en el centro, esa persona no podrá continuar registrando operaciones aunque haya dejado la aplicación abierta.

Los tiempos indicados son parámetros iniciales del producto y pueden ajustarse. Una actualización automática de la pantalla no se considera actividad del usuario.

### RN-03. Los registros utilizados se desactivan para conservar su historial

Si un usuario, profesional, servicio o consultorio ya está relacionado con operaciones del sistema, no se elimina físicamente. Cuando deja de utilizarse, se desactiva para impedir nuevas operaciones, conservando las referencias de las anteriores.

Además, siempre debe quedar al menos una cuenta activa con el rol de administrador. El sistema impedirá desactivar o retirar ese rol a la última cuenta que lo tenga.

**Ejemplo:** cuando un profesional deja de trabajar en el centro, se desactiva su ficha. Sus citas y atenciones anteriores siguen mostrando quién las realizó.

### RN-04. Cada paciente debe tener un registro único

El sistema asigna a cada paciente un código interno único. También impide crear dos fichas con la misma combinación de tipo y número de documento. Tener el mismo nombre no significa que dos pacientes sean la misma persona.

Los nombres, apellidos y fecha de nacimiento son obligatorios. La fecha de nacimiento no puede estar en el futuro. En esta versión se admite DNI de 8 dígitos y carné de extranjería de 9 caracteres alfanuméricos como formatos internos configurables; no se incluye el registro sin documento.

**Ejemplo:** si recepción intenta registrar un DNI que ya pertenece a una ficha, el sistema rechaza el duplicado para que se utilice el registro existente.

**Alcance de la validación:** comprobar el formato no confirma la identidad de la persona ni la existencia del documento. No se consulta un servicio oficial. Los tipos de documento y sus formatos son decisiones iniciales pendientes de revisión, no una afirmación sobre todos los documentos válidos en el país.

### RN-05. Corregir los datos de un paciente debe conservar su historial y los cambios de otros usuarios

Actualizar el nombre, teléfono o documento de un paciente no cambia su código interno ni elimina sus relaciones con citas, atenciones, cargos o pagos.

Si dos personas abren la misma ficha y una guarda un cambio, el sistema debe advertir a la otra antes de permitir que guarde una versión anterior. No se debe reemplazar silenciosamente información recién actualizada.

**Ejemplo:** una recepcionista actualiza el teléfono mientras otra tiene abierta la ficha. Cuando la segunda intenta guardar, debe revisar la versión actualizada para evitar perder el nuevo teléfono.

### RN-06. Una cita solo se registra si todos los recursos necesarios están disponibles

Antes de confirmar una cita, el sistema comprueba que:

- El paciente esté registrado.
- El profesional, el servicio y el consultorio estén activos.
- El profesional pueda atender el servicio seleccionado y el consultorio sea compatible con él.
- Todo el tiempo necesario para la cita esté dentro de la disponibilidad del profesional y del consultorio.
- La reserva no corresponda a una fecha u hora pasada.

**Ejemplo:** si un profesional termina su turno a las 12:00, no se puede reservar a las 11:45 un servicio que dura 30 minutos, porque terminaría fuera de su horario.

### RN-07. No se pueden reservar citas que se crucen para el mismo paciente, profesional o consultorio

El sistema impide que un paciente, profesional o consultorio participe en dos reservas que ocupen parte del mismo horario. Para esta comprobación se consideran las citas que no estén canceladas ni marcadas como ausentes.

Una cita puede comenzar exactamente cuando termina la anterior. No es necesario dejar un espacio adicional salvo que esté incluido en la duración del servicio o en los horarios configurados.

**Ejemplo:** una cita de 9:00 a 9:30 es compatible con otra de 9:30 a 10:00. En cambio, una cita de 9:15 a 9:45 se cruza con ambas si utiliza el mismo recurso.

**Indicación técnica:** esta protección debe funcionar aunque dos usuarios confirmen una reserva al mismo tiempo. La base de datos debe permitir confirmar solo la operación compatible; no basta con haber mostrado el horario como libre unos segundos antes.

### RN-08. Los cambios de disponibilidad deben resolver las citas que resulten afectadas

Cuando se registra una ausencia o un bloqueo que afecta citas existentes, el sistema identifica esas citas y evita nuevas reservas en el horario afectado. Recepción debe reprogramarlas o cancelarlas; el sistema no toma esa decisión automáticamente.

Mientras existan citas por resolver, el bloqueo queda pendiente. Esto significa que ya no se permiten nuevas reservas, pero todavía falta gestionar las anteriores. El bloqueo se completa o el recurso se desactiva cuando se han resuelto todas las citas afectadas.

Si hay una atención en curso, el sistema rechaza un bloqueo inmediato que se cruce con ella. La gestión de disponibilidad no interrumpe una atención iniciada.

**Ejemplo:** un profesional informa que no atenderá mañana y tiene seis citas. El sistema impide reservar nuevas citas en su turno y muestra las seis pendientes para que recepción gestione cada una.

### RN-09. Las citas deben seguir un recorrido de estados permitido

El recorrido habitual es: **PROGRAMADA → EN_ESPERA → EN_ATENCION → ATENDIDA**. También puede registrarse una cancelación o una ausencia bajo las condiciones de la sección 5.

Solo una cita PROGRAMADA puede reprogramarse. Las citas CANCELADA, AUSENTE y ATENDIDA se consideran cerradas en esta versión: no se reutilizan para registrar una nueva atención. Las operaciones que no estén previstas en la tabla de estados se rechazan.

**Ejemplo:** no se puede marcar una cita como atendida sin haber registrado antes el inicio de la atención. Si una cita fue cancelada y el paciente solicita otra fecha, se registra una nueva cita.

El procedimiento para corregir un estado registrado por error todavía no forma parte de esta versión y deberá definirse antes de habilitar ese tipo de cambios.

### RN-10. Las llegadas y los tiempos de atención deben registrarse una sola vez y en orden

La llegada se registra únicamente en la fecha de la cita, según la hora local del centro. El inicio de atención no puede preceder a la llegada y la finalización no puede preceder al inicio.

Solo se marca ausencia cuando ya terminó el horario programado y no existe una llegada registrada. Si una operación se repite por un doble clic o un reintento, se conserva el momento original: no se crea otra llegada ni se reinicia la espera.

**Ejemplo:** si el paciente llegó a las 9:05 y la atención comenzó a las 9:25, su espera fue de 20 minutos. Pulsar nuevamente el botón de llegada no debe cambiar ese cálculo.

**Indicación técnica:** los momentos los registra el servidor para evitar diferencias entre los relojes de las computadoras. Este seguimiento no determina urgencias ni prioridades clínicas.

### RN-11. Las agendas y los cierres deben utilizar la hora local del centro

Las fechas que ven los usuarios y los periodos de los reportes se interpretan con la hora de Perú. Una operación debe pertenecer al día correcto del centro, independientemente del país donde esté alojado el servidor.

**Ejemplo:** un pago registrado a las 11:30 p. m. del lunes en Perú debe aparecer como movimiento del lunes, aunque en la ubicación del servidor ya sea martes.

**Indicación técnica:** los momentos se almacenan con referencia UTC y se presentan utilizando la zona horaria America/Lima. Esta conversión también se aplica al filtrar agendas y movimientos de caja.

### RN-12. Una reserva conserva la duración y las condiciones económicas con las que se registró

La cita guarda la duración del servicio, la modalidad de atención y el importe que correspondían al momento de reservar. Si después cambia la tarifa o la duración del catálogo, las citas existentes no se modifican automáticamente.

Reprogramar una cita conserva esas condiciones. Recepción puede seleccionar las modalidades configuradas por el administrador, pero no introducir descuentos libres. Una modalidad sin cargo no genera deuda.

**Ejemplo:** una cita reservada por S/40 conserva ese importe aunque la tarifa del servicio cambie después a S/45. Las nuevas reservas utilizan la tarifa actualizada.

### RN-13. Los cargos, pagos y saldos deben mantener importes consistentes

El **cargo** es el importe que corresponde cobrar por el servicio; el **pago** es el importe registrado como recibido; y el **saldo** es lo que queda pendiente.

Se permiten pagos parciales, pero no pagos superiores al saldo ni dos cargos vigentes por la misma cita. Si se revierte un pago, su importe deja de descontarse del cargo y el saldo se recalcula. Tener un saldo pendiente no bloquea automáticamente la atención.

**Ejemplo:** un cargo de S/80 con un pago de S/30 deja S/50 pendientes. Si ese pago se revierte por completo y el cargo continúa vigente, el saldo vuelve a S/80.

**Indicación técnica:** los importes se expresan en soles, con dos decimales, y se calculan con precisión decimal. Las comprobaciones también deben funcionar cuando dos cajeros intentan cobrar el mismo saldo simultáneamente.

### RN-14. Las anulaciones económicas necesitan un motivo y la aprobación de otro usuario

Para dejar sin efecto un cargo o un pago, caja registra una solicitud con el motivo y la operación original. Una cuenta distinta, con rol de administrador, debe aprobarla o rechazarla. El solicitante no puede aprobar su propia solicitud aunque también tenga ese rol.

La aprobación conserva el registro original y añade un movimiento vinculado que compensa su efecto. Si se devuelve dinero al paciente, se registra la devolución y la referencia del medio utilizado. El sistema registra lo ocurrido, pero no ejecuta transferencias bancarias.

Cancelar una cita no anula automáticamente cargos ni devuelve pagos. Una operación que ya fue revertida por completo no puede revertirse otra vez.

**Ejemplo:** caja solicita corregir un pago registrado incorrectamente. Otro usuario autorizado revisa el motivo y aprueba la corrección; quedan visibles tanto el pago original como el movimiento que lo deja sin efecto.

Corregir un registro equivocado y devolver dinero realmente recibido son situaciones diferentes; al registrar la operación debe quedar claro cuál ocurrió.

### RN-15. Los movimientos de cada cajero deben quedar dentro de una caja abierta y conservarse después del cierre

Cada cajero puede tener una sola sesión de caja abierta. Una sesión es el periodo comprendido entre su apertura y su cierre. Todo cobro o devolución debe asociarse a una sesión abierta.

Al cerrar, el sistema calcula cuánto efectivo debería existir y lo compara con lo contado por el cajero:

- **Efectivo esperado:** fondo inicial + cobros en efectivo − devoluciones en efectivo.
- **Diferencia:** efectivo contado por el cajero − efectivo esperado.

Los pagos por otros medios se revisan por separado, porque no aumentan el dinero físico de la caja. Una vez cerrado, el resumen no se modifica ni recibe operaciones adicionales. Las correcciones posteriores se registran en una nueva sesión y se vinculan con el movimiento anterior.

**Ejemplo:** si la caja abre con S/100, cobra S/300 en efectivo y devuelve S/40, debe contener S/360. Si el cajero cuenta S/350, se registra una diferencia de −S/10 y el motivo correspondiente.

**Indicación técnica:** si un cobro y un cierre llegan al mismo tiempo, el sistema debe coordinarlos para que el cobro quede incluido antes del cierre o sea rechazado; nunca debe aparecer después dentro de una caja ya cerrada.

### RN-16. Los reportes deben indicar qué cuentan y a qué periodo corresponden

Cada indicador se calcula siguiendo la definición de la sección 6. Los reportes operativos utilizan la fecha de la cita y los económicos la fecha en que se registró el movimiento de dinero.

Los reportes deben mostrar sus filtros y distinguir conceptos como cargos, cobros, devoluciones y saldos. Los promedios y porcentajes deben indicar qué registros se tomaron en cuenta. Cuando no hay datos suficientes, se muestra “sin datos”.

**Ejemplo:** una cita programada para el viernes y pagada el miércoles aparece en la agenda del viernes, pero el cobro pertenece al reporte económico del miércoles. No se debe mover el ingreso al viernes solo para hacerlo coincidir con la atención.

### RN-17. Las operaciones importantes deben dejar un historial de quién las realizó

El sistema conserva un registro de auditoría para reconstruir las operaciones relevantes: quién actuó, qué acción realizó, sobre qué registro, en qué momento y con qué resultado.

Este historial incluye acciones como modificar permisos, cambiar una cita, actualizar tarifas o autorizar una anulación. Los usuarios de la aplicación no pueden borrar estos eventos.

**Ejemplo:** si una cita aparece cancelada, el administrador puede consultar qué cuenta la canceló, cuándo ocurrió y cuál fue el motivo registrado.

**Indicación técnica:** la auditoría utiliza identificadores y los datos mínimos necesarios. No guarda contraseñas, credenciales de sesión ni copias completas de información personal. La auditoría permite revisar acciones; no sustituye las copias de respaldo.

Los parámetros de sesión y de validación documental son decisiones iniciales del producto. Los ejemplos explican las reglas y no representan operaciones ya realizadas ni obligaciones legales.

## 5. Estados y transiciones

### 5.1 Cita y atención administrativa

| Estado origen | Operación | Estado destino | Actor / condición |
|---|---|---|---|
| Sin cita | Programar | PROGRAMADA | REC; disponibilidad válida |
| PROGRAMADA | Reprogramar | PROGRAMADA | REC; reserva nueva válida; conservar historial |
| PROGRAMADA | Registrar llegada | EN_ESPERA | REC; fecha local de la cita |
| PROGRAMADA | Cancelar | CANCELADA | REC; motivo obligatorio |
| PROGRAMADA | Registrar ausencia | AUSENTE | REC; fin programado superado y sin llegada |
| EN_ESPERA | Iniciar | EN_ATENCION | PRO asignado; sin conflicto pendiente |
| EN_ESPERA | Registrar retiro | CANCELADA | REC; motivo de retiro; conservar llegada |
| EN_ATENCION | Finalizar | ATENDIDA | PRO asignado |

Las operaciones no contempladas se rechazan. El inicio previsto no es una garantía de inicio real; no se alteran automáticamente otras citas por un retraso. Un profesional no inicia dos atenciones a la vez y un consultorio no admite dos atenciones activas simultáneas. Una cita atendida fuera de su intervalo conserva tanto su programación como sus tiempos reales.

### 5.2 Movimientos económicos

- Cargo: PENDIENTE, PARCIAL o PAGADO según saldo; ANULADO después de su resolución autorizada.
- Solicitud de reversión: PENDIENTE → APROBADA o RECHAZADA. Una aprobación ejecuta un único movimiento compensatorio dentro de la misma transacción; si falla, no queda aprobada parcialmente.
- Sesión de caja: ABIERTA → CERRADA. No existe reapertura ni modificación retroactiva del cierre en esta versión.

## 6. Definición de indicadores

| Indicador | Definición |
|---|---|
| Citas por estado | Cantidad de citas con fecha programada dentro del periodo, agrupadas por estado actual. |
| Personas en espera | Citas EN_ESPERA en la fecha consultada. |
| Atenciones realizadas | Citas ATENDIDA del periodo seleccionado. |
| Ausencias | Citas AUSENTE del periodo; tasa = AUSENTE / (AUSENTE + ATENDIDA), informando el denominador y excluyendo abiertas y canceladas. |
| Espera promedio | Promedio de inicio real − llegada para registros con ambos momentos. No incluye ausentes ni retiros sin inicio; se informa la cantidad de registros utilizados. |
| Duración de atención | Fin real − inicio real para atenciones finalizadas. |
| Ocupación programada | Minutos de reserva de citas no canceladas ni ausentes dentro de la disponibilidad efectiva / minutos de disponibilidad efectiva del consultorio en el periodo. Se descuentan bloqueos y descansos; se recortan intervalos al periodo. Los conflictos pendientes se informan aparte. No mide uso físico real. |
| Ingresos netos | Cobros − devoluciones por fecha de movimiento; excluye fondo inicial y cargos no cobrados. |
| Saldos pendientes | Suma de saldos vigentes al momento de la consulta, indicando fecha de corte. No se interpreta como saldo histórico reconstruido. |

Un denominador cero o una muestra vacía se presenta como “sin datos”, no como un resultado numérico engañoso.

## 7. Requisitos de calidad y operación

Los objetivos siguientes son condiciones de aceptación del producto. Sus resultados deberán registrarse; no son niveles de servicio ya garantizados.

| ID / prioridad | Requisito y objetivo medible | Verificación |
|---|---|---|
| RNF-01 · P8 | Autorizar cada operación protegida y su recurso; impedir lectura o escritura al cambiar IDs o invocar directamente la API sin permisos. | Pruebas positivas y negativas de las filas de la matriz; incluir combinaciones de roles y cuentas desactivadas. |
| RNF-02 · P8 | Contraseñas de al menos 12 caracteres almacenadas con hash adaptativo con sal; HTTPS en despliegue; ningún secreto en repositorio, respuestas ni logs. Limitar intentos de acceso: 5 fallos por cuenta en 15 minutos provocan espera de 15 minutos, con mensaje genérico. | Inspección de configuración y pruebas de sesión, restablecimiento y limitación de intentos; el algoritmo y parámetros se documentan en el ADD. |
| RNF-03 · P8 | Evitar duplicaciones por reintentos y escrituras parciales en reservas, pagos, reversiones y cierres. | Pruebas de al menos 10 solicitudes concurrentes incompatibles: una reserva confirmada; sin sobrepago, doble reversión ni movimientos posteriores al cierre. Simular fallo durante transacción y verificar reversión completa. |
| RNF-04 · P8 | En un entorno de referencia de 4 vCPU y 8 GiB para API y base de datos, con 10 000 pacientes, 50 000 citas y 20 usuarios concurrentes durante 10 minutos: p95 de búsquedas, agendas y escrituras ordinarias ≤ 2 s; errores inesperados < 1 %. | Prueba con mezcla 70 % lecturas y 30 % escrituras, calentamiento previo y red local; registrar configuración. Los rechazos esperados de validación no cuentan como fallos inesperados. |
| RNF-05 · P8 | Proporcionar logs, métricas y trazas correlacionadas de reserva y pago en la plataforma de observabilidad seleccionada, sin documentos, nombres completos, credenciales ni cuerpos sensibles. | Ejecutar una operación exitosa y una fallida, localizar su correlación y revisar que la telemetría no expone datos excluidos. La caída de la plataforma de observabilidad no bloquea operaciones del negocio. |
| RNF-06 · P8 | Reproducir el entorno mediante configuración declarativa, dependencias fijadas, migraciones versionadas y guía de arranque; separar configuración y secretos del código. | Levantar desde una copia limpia, aplicar migraciones y ejecutar verificaciones documentadas sin pasos ocultos. |
| RNF-07 · P8 | Mantener reglas del negocio independientes de React, FastAPI y persistencia; disponer de instrucciones compartidas versionadas y evidencias del asistente realmente utilizado. | Revisar dependencias del dominio y probar reglas sin servidor HTTP. Registrar tarea asistida, cambio, revisión y comprobaciones; no exigir uso de Claude Code si se usa Codex. |
| RNF-08 · P8 | Permitir completar registro, reserva y pago mediante teclado; mostrar etiquetas, foco visible, errores junto al campo y estados no basados solo en color. | Recorrer los tres flujos en versiones estables actuales de Chrome y Firefox, registrando versiones y sin errores bloqueantes. Verificar también a 1366 × 768. Ejecutar Axe-core sobre estados iniciales, errores, diálogos y resultados: resolver hallazgos críticos/serios, documentar los demás y revisar resultados indeterminados. Complementar con revisión manual de teclado y foco; no afirmar accesibilidad completa por ausencia de errores automáticos. |
| RNF-09 · P8 | Detectar indisponibilidad de la base de datos en el indicador de preparación y evitar confirmaciones falsas; restablecer servicio tras reiniciar la API sin perder operaciones confirmadas. | Interrumpir conexión durante una escritura y reiniciar; respuesta de fallo comprensible y estado consistente al consultar de nuevo. |
| RNF-10 · P8 | Respaldo diario con conservación de 7 copias y recuperación ensayada: pérdida máxima objetivo 24 h, restauración objetivo ≤ 60 min sobre el conjunto de referencia. | Restaurar una copia en entorno separado y verificar pacientes, reservas, movimientos y auditoría. Registrar tiempo y fecha del respaldo. |
| RNF-11 · F16 | Reportes detallados de hasta 12 meses y exportaciones de hasta 10 000 filas en ≤ 10 s sobre el conjunto y entorno de RNF-04. | Medición reproducible con filtros representativos y validación de totales y permisos. |
| RNF-12 · P8 | Las pruebas de las reglas de superposición, saldo, aprobación de reversiones y cálculo de cierre deben detectar alteraciones que cambien su comportamiento. | Ejecutar pruebas de mutación sobre esas funciones del dominio. Conservar resultados detectados, supervivientes, equivalentes, excluidos y no ejecutables. Resolver supervivientes que revelen fallos de verificación y justificar equivalentes/exclusiones. No sustituye las pruebas de concurrencia de RNF-03. |

No se establece disponibilidad porcentual mensual sin definir antes infraestructura y periodo de observación. La selección concreta de autenticación, despliegue y respaldo se justificará en el ADD cumpliendo estos requisitos.

## 8. Dependencias y alcance de entregas

### 8.1 Dependencias principales

- RF-01 a RF-03 habilitan el acceso de los demás módulos.
- RF-04, RF-07 a RF-10 preceden a RF-13; RF-11 amplía el manejo de cambios de disponibilidad.
- RF-13 precede a RF-14 a RF-18 y RF-19.
- RF-19 y RF-20 preceden a RF-21; RF-21 alimenta RF-22 a RF-24 y RF-27.
- RF-28 es transversal y debe incorporarse con cada operación relevante, no al terminar los módulos.
- RF-25 a RF-27 se basan en eventos y estados confirmados; RF-29 depende del reporte a exportar.

### 8.2 Objetivo de la presentación parcial

La meta P8 comprende un sistema integrado con configuración de recursos, permisos, pacientes, citas, admisión, atención administrativa, caja con cierre, dashboard, reporte económico y auditoría. Incluye RF-01 a RF-25, RF-27 y RF-28, más RNF-01 a RNF-10 y RNF-12. Es una meta amplia: debe convertirse en tareas estimadas antes de comprometer un calendario individual o grupal.

La demostración deberá recorrer al menos:

1. Configuración de un servicio, profesional, disponibilidad y consultorio.
2. Registro y búsqueda de paciente; rechazo de duplicado.
3. Reserva válida y rechazo de conflicto; reprogramación consistente.
4. Llegada, inicio y finalización; consulta del dashboard.
5. Cargo, pago parcial y saldo; pago restante y cierre de caja.
6. Atención sin cargo que avanza sin pago obligatorio.
7. Solicitud y autorización de reversión, con controles de identidad y conservación del cierre previo cuando corresponda.
8. Acceso rechazado por permisos, consulta de auditoría y localización de una traza.

La preparación de exposición se rige por [PRESENTACION_PARCIAL.md](PRESENTACION_PARCIAL.md): además del software, se requieren descripción, principales requerimientos, matriz de tendencias, ADD y conclusiones del uso real de las tendencias.

### 8.3 Complemento para entrega final

RF-26, RF-29 y RNF-11 amplían análisis y exportación. La entrega final también incorpora correcciones de la revisión parcial, resolución de incidencias y validación de la implementación completa. Insumos no se agrega automáticamente: requiere una decisión explícita de alcance.

## 9. Trazabilidad y criterio de finalización

Cada tarea de implementación deberá mencionar los RF, RN y RNF aplicables. Cada prueba funcional debe identificar su criterio, por ejemplo RF-14/CA-2. El conjunto de verificaciones incluirá permisos, casos de negocio, concurrencia e integridad económica.

Un requerimiento se considera terminado cuando:

1. Sus funciones y validaciones están implementadas en los canales correspondientes.
2. Sus criterios de aceptación y reglas relacionadas se verifican con evidencia reproducible.
3. Se respetan la matriz de permisos y las condiciones de calidad aplicables.
4. Se actualizan las instrucciones de uso o configuración y el ADD cuando cambia una decisión arquitectónica.

La [matriz de tendencias vs. requerimientos](03_MATRIZ_TENDENCIAS_REQUERIMIENTOS.md) utiliza estos identificadores y define las evidencias previstas. Este documento no sustituye esa matriz ni el ADD.
