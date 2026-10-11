# Citas y atención

El workspace autenticado cubre agenda diaria, programación, reprogramación, llegada, sala de espera, inicio/finalización de atención, cancelación, ausencia, retiro de espera y dashboard operativo. La API y sus tipos están en `api/appointments.ts`; la composición de las vistas está en `components/AppointmentsWorkspace.tsx`.

La vista de atención actualiza el tiempo de espera en el navegador a partir del timestamp emitido por el servidor, pero cada transición se valida y persiste en backend. Las acciones se muestran según las capacidades recibidas en `/api/citas/contexto`.

El modo demo se activa en backend con `APPOINTMENTS_MOCK_ENABLED=true`. La cuenta local tiene temporalmente capacidades de recepción y del profesional mock asignado. Los cierres muestran el tipo, motivo, momento y, cuando el puerto lo indique, una advertencia de gestión económica pendiente. La vista semanal, reaperturas, caja y reportes detallados se implementarán posteriormente.
