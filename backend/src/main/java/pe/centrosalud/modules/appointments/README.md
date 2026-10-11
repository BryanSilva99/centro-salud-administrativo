# Citas, admisión y atención

El módulo implementa agenda diaria, programación, reprogramación, llegada, espera, inicio/finalización de atención, cancelación, ausencia, retiro de espera y dashboard operativo (RF-12 a RF-18 y RF-25). El dominio no depende de Spring ni de JPA; las referencias a pacientes y recursos llegan por puertos.

## API autenticada

- `GET /api/citas/contexto`: catálogos reales mínimos, capacidades y vínculo profesional vigente.
- `GET /api/citas/agenda?fecha=YYYY-MM-DD&profesionalId=&servicioId=&consultorioId=`: agenda diaria.
- `GET /api/citas/disponibilidad`: intervalos libres en pasos de 15 minutos.
- `POST /api/citas`: programa una cita.
- `POST /api/citas/{id}/reprogramaciones`: cambia horario conservando identidad e historial.
- `GET /api/citas/atencion?fecha=YYYY-MM-DD&profesionalId=`: cola de espera y atenciones activas.
- `PUT /api/citas/{id}/llegada`: registra llegada con `{ "version": n }`.
- `PUT /api/citas/{id}/atencion/inicio`: inicia la atención asignada.
- `PUT /api/citas/{id}/atencion/finalizacion`: finaliza la atención asignada.
- `PUT /api/citas/{id}/cancelacion`: cancela una cita programada con `{ "motivo": "...", "version": n }`.
- `PUT /api/citas/{id}/ausencia`: registra una ausencia con `{ "version": n }` cuando el intervalo terminó.
- `PUT /api/citas/{id}/espera/retiro`: cierra una espera con `{ "motivo": "...", "version": n }`.
- `GET /api/citas/dashboard?fecha=&profesionalId=&servicioId=&consultorioId=`: conteos diarios por estado.

Todas las rutas requieren sesión. Las operaciones de escritura requieren el token CSRF vigente. Las transiciones son idempotentes: un reintento devuelve las marcas temporales originales.

## Persistencia

Las citas y reprogramaciones se almacenan en `citas` y `reprogramaciones_cita`. La migración V16 agrega actores y timestamps de llegada/atención, y V17 agrega tipo, motivo, actor y momento de cierre con restricciones de coherencia. Los bloqueos transaccionales y el trigger PostgreSQL liberan los recursos cuando una cita queda cancelada o ausente. Las condiciones económicas de la reserva permanecen congeladas.

## Integración real

Los adaptadores de `adapters/outbound/modules/appointments` consultan los contratos públicos de administración e identidad. Las fixtures solo se compilan en pruebas: `APPOINTMENTS_MOCK_ENABLED` ya no activa permisos ni catálogos ficticios.

REC gestiona reservas y admisión. ADM consulta agenda, espera e indicadores globales, sin acceso al directorio de pacientes ni reserva. PRO necesita una ficha activa vinculada y solo consulta/atiende sus citas. Los roles combinados suman consultas, pero iniciar/finalizar atención siempre exige el profesional asignado.

La disponibilidad intersecta los turnos DISPONIBLE de ambos recursos, comprueba servicios habilitados y compatibilidad, duración y reservas. Los intervalos contiguos se unen; los horarios ofrecidos se alinean a cuartos de hora. Los turnos DESCANSO no son reservables.

V15–V17 continúan las migraciones V1–V14 sin reescribirlas. El trigger de V15 serializa las reservas por paciente, profesional y consultorio y rechaza solapes dentro de PostgreSQL, sin extensiones. No renumerar migraciones que ya se hayan aplicado en otra base; las bases antiguas de demostración requieren una migración planificada aparte.

## Pendientes

Caja se integra en el paso 3. Una cancelación o retiro de una reserva con importe informa que requiere revisión económica; no consulta saldos ni crea movimientos. No se simula que una reserva con importe ya esté conciliada.

El inicio de atención verifica que el horario siga cubierto y los recursos habilitados. El flujo de bloqueos pendientes y resolución de impacto RF-11, vista semanal, reaperturas y auditoría global siguen pendientes. La desactivación administrativa aún no presenta ni resuelve citas afectadas (RN-08); esta integración no declara completos esos criterios.

Ver [informe de integración](../../../../../../../../../docs/INTEGRACION_CITAS.md).
