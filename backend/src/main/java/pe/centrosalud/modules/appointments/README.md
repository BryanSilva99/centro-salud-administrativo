# Citas, admisión y atención

El módulo implementa agenda diaria, programación, reprogramación, llegada, espera, inicio/finalización de atención, cancelación, ausencia, retiro de espera y dashboard operativo (RF-12 a RF-18 y RF-25). El dominio no depende de Spring ni de JPA; las referencias a pacientes y recursos llegan por puertos.

## API autenticada

- `GET /api/citas/contexto`: catálogos sintéticos, capacidades y profesional demo asignado.
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

Las citas y reprogramaciones se almacenan en `citas` y `reprogramaciones_cita`. La migración V6 agrega actores y timestamps de llegada/atención, y V7 agrega tipo, motivo, actor y momento de cierre con restricciones de coherencia. Las exclusiones PostgreSQL liberan los recursos cuando una cita queda cancelada o ausente. Las condiciones económicas de la reserva permanecen congeladas.

## Modo demo local

`APPOINTMENTS_MOCK_ENABLED=true` habilita el catálogo determinista y una autorización temporal. Cada cuenta autenticada actúa como recepción y como `PROFESIONAL_1`; puede registrar llegadas, cancelaciones, ausencias y retiros generales, pero solo iniciar y finalizar citas de ese profesional. Con `false`, los adaptadores deniegan operaciones hasta integrar identidad, personal y catálogos reales. Las fixtures no se insertan en PostgreSQL.

La vista semanal, las correcciones o reaperturas, caja, auditoría global y los indicadores detallados de RF-26 siguen fuera de esta iteración. El puerto de impacto económico solo informa si queda una gestión pendiente; no crea movimientos de caja.
