# Citas y atención

El workspace autenticado cubre agenda diaria, programación, reprogramación, llegada, sala de espera, inicio/finalización de atención, cancelación, ausencia, retiro de espera y dashboard operativo. La API y sus tipos están en `api/appointments.ts`; la composición de las vistas está en `components/AppointmentsWorkspace.tsx`.

La vista de atención actualiza el tiempo de espera en el navegador a partir del timestamp emitido por el servidor, pero cada transición se valida y persiste en backend. Las acciones se muestran según las capacidades recibidas en `/api/citas/contexto`.

La vista se abre desde el menú de SessionView y reutiliza su sesión y cierre. REC puede reservar; ADM consulta; PRO atiende las citas de su ficha vinculada. Los permisos se verifican de nuevo en el servidor. Los catálogos proceden de administración y no se habilita modo demo.

Caja, vista semanal y el flujo de bloqueos con citas afectadas siguen pendientes. La adaptación visual detallada a Figma no se incluye en esta integración; los estilos de citas quedan limitados al componente para conservar la interfaz de main.
