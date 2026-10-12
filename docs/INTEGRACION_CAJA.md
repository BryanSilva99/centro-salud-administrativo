# Integración de caja — paso 3

11 de octubre de 2026. Rama local `integration/caja`, sobre `integration/citas` (`ff14c3a`), incorporando `origin/feature/billing-jj` (`5e62e75`).

## Integración y correcciones

- Caja aparece dentro de la navegación y sesión existentes, con permiso `CAJA_CONSULTAR` para CAJ/ADM. Los permisos actuales se verifican en el servidor en cada operación. ADM consulta y resuelve solicitudes ajenas; solo CAJ registra cargos, abre su caja, cobra, solicita reversiones y cierra su sesión.
- El actor se obtiene de la sesión autenticada. No se aceptan IDs de cajero/solicitante/aprobador desde el formulario. El cargo obtiene paciente e importe de la reserva mediante contratos públicos; la lectura se bloquea contra una cancelación concurrente. No se consulta la tarifa actual para alterar una reserva anterior.
- Modalidad gratuita: importe cero, sin deuda ni pago obligatorio. Cita cancelada: no admite nuevos cargos. Reintentar generación devuelve el cargo vigente, incluyendo PAGADO. Cancelar o retirar una cita avisa de los cargos vigentes reales y no revierte dinero automáticamente.
- Se corrige el cálculo original de pago, que descontaba el importe antes de comprobar el saldo. Saldo = cargo − cobros + devoluciones; un cargo anulado tiene saldo cero. Importes no negativos (pagos positivos), máximo dos decimales; referencias obligatorias en medios no efectivos.
- Pagos y aperturas tienen clave de operación persistida. Un reintento con la misma clave/datos devuelve el resultado; otros datos con esa clave producen conflicto. El frontend conserva la clave ante error de red. Cierre y resolución repetidos con el mismo resultado no duplican movimientos.
- Aprobar una reversión de pago crea una devolución vinculada al original, dentro de la caja abierta del solicitante. No modifica la sesión original cerrada. Anular un cargo exige resolver primero sus pagos vigentes y conserva una compensación vinculada a la solicitud. El solicitante no puede resolver su propia solicitud, incluso con CAJ+ADM.
- Una transacción engloba comprobaciones y escrituras. Una fila `control_caja` serializa operaciones de caja entre instancias, evitando sobrepago, doble apertura/reversión y movimientos posteriores al cierre. Se eligió una cola sencilla para el volumen del centro; no está diseñada para alta concurrencia. Las pruebas fuerzan fallos para verificar rollback.
- Reporte por fecha local de movimiento, límite superior exclusivo. CAJ solo obtiene movimientos de sus sesiones; ADM consulta el centro. El detalle conserva medio, referencia, cobros y devoluciones; fondo inicial y deuda no cobrada se distinguen de ingresos.
- El cliente usa el mecanismo compartido de CSRF y la sesión existente. Selecciona citas reales, recupera cajas abiertas/cierres y muestra solicitudes a ADM; no requiere copiar UUID para operar.

## Migración

La V2 de la rama original se renumera **V18 antes de aplicarla en la base integrada**. V1–V17 se conservan sin cambios. V18 añade tablas de caja, índices únicos de cargo vigente/caja abierta, reintentos y compensaciones. Una base independiente que ya ejecutó la antigua V2 de J.J. necesita su propia transición: no aplicar `repair` a ciegas.

## Comprobación

- Backend: `./mvnw verify`, **224 pruebas aprobadas, cero fallos/errores/omisiones**; Spotless y empaquetado correctos.
- Frontend: **85 pruebas aprobadas**; comprobación TypeScript y compilación de producción correctas.
- Nueve recorridos HTTP nuevos con login/CSRF reales y esquemas PostgreSQL aislados: tarifa conservada, IDs falsificados ignorados, rol revocado, caja ajena, sin cargo, cancelación, reintentos, apertura/cobro/reversión/cierre concurrentes y rollback de pago/devolución.
- Diez solicitudes incompatibles simultáneas: un cobro total, una apertura y una devolución efectiva. Carrera entre diez cobros y cierre: el resumen incluye únicamente los confirmados antes del cierre.
- Base local: Flyway valida 18 migraciones y aplica V18 sobre V17; backend reiniciado y disponible en puerto 8080.
- Navegador: login con cuenta local existente, menú Caja y cobros y consulta de reporte vacío correctos. La cuenta local conserva ADM/PRO/REC: consulta y aprobación mediante ADM; para cobrar se requiere una cuenta CAJ.

Ejecución backend: `RUN_POSTGRES_TESTS=true`, `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=4`, `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=1`, y esquema de pruebas separado del público. Se retiró el esquema compartido de prueba al terminar. No se crearon movimientos de prueba en la base pública.

## Límites que siguen pendientes

- La operación de reversión de pago implementada es **devolución completa**. No ofrece todavía un flujo separado de corrección contable sin devolución física; no se afirma RF-23 completo para ambas situaciones.
- Búsqueda económica avanzada por paciente/estado, paginación de historiales, exportación y auditoría global permanecen pendientes. La vista actual permite seleccionar citas por fecha y consultar sus cargos/movimientos.
- La interfaz conserva el contenedor y estilos del sistema; no se declara adaptación visual completa a Figma.
- Las claves del frontend se conservan durante la vista, no después de recargar el navegador. Ante pérdida de respuesta y recarga, revisar movimientos antes de crear otra operación. El servidor conserva las claves ya registradas.
- Continúan los pendientes de administración/citas documentados en el paso 2 (RF-11/RN-08, vista semanal y auditoría). Esta integración no equivale a la aceptación completa de todos los requisitos.
- El paso 4 corresponde a la validación conjunta y pendientes acordados; el paso 5 a publicar la rama/PR e integrar en main. Esta etapa no modifica las ramas remotas.
