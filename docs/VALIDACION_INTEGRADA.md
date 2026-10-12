# Validación integrada — paso 4

12 de octubre de 2026. Rama `integration/validacion`, sobre `integration/caja` (`0a27c46`).

## Recorrido comprobado

`SistemaIntegradoHttpTest` inicia con una única cuenta ADM sintética. Crea las demás cuentas y recursos mediante HTTP autenticado con CSRF, en un esquema PostgreSQL aislado. No inserta pacientes, citas, cargos ni movimientos directamente en la base.

| Escenario | Comprobación |
|---|---|
| Identidad y configuración | ADM crea cuentas REC/PRO/CAJ, servicio de 30 minutos con modalidades particular/gratuita, profesional y consultorio compatibles, vínculo PRO y turnos coincidentes. Se usan las versiones retornadas por la API. |
| Paciente | REC registra y busca al paciente; duplicar su documento devuelve conflicto. Cambiar su nombre conserva los IDs y relaciones con cita y cargo. |
| Reserva y tarifa histórica | Reserva particular por S/40; ADM cambia catálogo a S/70 y 45 minutos; reprogramar conserva S/40 y 30 minutos. |
| Atención con deuda | Pago parcial S/15 deja S/25; REC registra llegada y el PRO vinculado inicia/finaliza atención sin exigir liquidar el saldo. Agenda y dashboard muestran ATENDIDA. |
| Cobro y cierre | Segundo pago S/25 liquida el cargo; con fondo S/100, cerrar con S/140 concilia sin diferencia. |
| Devolución posterior | Solicitar devolución del primer pago no altera el saldo. Aprobar sin caja abierta del solicitante se rechaza. En nueva caja, otro ADM aprueba: saldo S/15, cierre nuevo S/85 y cierre anterior S/140 intacto. |
| Reporte | Por fecha del movimiento: cobros S/40, devoluciones S/15, neto S/25; fondo inicial excluido. |
| Modalidad gratuita | Reserva y atención completa por S/0; generar deuda se rechaza y la consulta económica devuelve cero cargos. |
| Acceso entre módulos | REC/PRO no consultan caja; CAJ no obtiene ficha completa del paciente; PRO no amplía su agenda mediante un profesional ajeno en el parámetro. |
| Cambios de cuenta | Revocar CAJ por la API retira acceso usando la misma cookie. Desactivar PRO invalida su sesión. En React, revocar el permiso de caja retira la vista y conserva un solo cierre de sesión. |

Las pruebas previas de cada módulo complementan estos recorridos: conflictos de reserva y edición, pacientes duplicados concurrentes, recursos incompatibles/inactivos, sobrepago, reintentos, reversiones/cierres concurrentes y rollback ante errores de persistencia.

## Correcciones de esta etapa

- Desactivar un turno exige que pertenezca al profesional/consultorio indicado en la URL. Antes se ignoraba el recurso de la ruta y podía desactivarse un turno ajeno. La API devuelve 404 y conserva el turno original ante esa combinación.
- Se corrige la fixture de ausencia de `AppointmentsPostgresTest`: antes colocaba la cita a las 08:00 del mismo día y fallaba si se ejecutaba antes de las 08:30. Ahora la ausencia está en el día anterior y la llegada/retiro siguen en el día actual. La regla real de ausencia se conserva.

## Resultado de comprobaciones

- Backend: `./mvnw verify`, **227 pruebas aprobadas, cero fallos/errores/omisiones**; incluye Spotless y empaquetado.
- Frontend: **86 pruebas aprobadas**, TypeScript y build correctos.
- PostgreSQL: esquemas de prueba aislados; el esquema compartido `test_validacion_20261012` se retira al terminar. Sin cambios en migraciones V1–V18.
- Se restablecen PostgreSQL, backend y frontend locales en la rama validada para continuar las pruebas del usuario. Las cuentas y datos públicos existentes se conservan.

No se declara comparación visual con Figma ni validación manual completa en Chrome/Firefox.

## Requisitos pendientes de aceptación

Esta etapa valida la unión de las ramas; no convierte funciones todavía ausentes en requisitos aprobados.

| Pendiente | Consecuencia para la entrega |
|---|---|
| RF-11 / RN-08: preparar bloqueos y resolver citas afectadas al desactivar recursos/turnos | La atención ya rechaza recursos que dejaron de estar disponibles, pero falta listar y resolver el impacto antes del cambio. Requiere un flujo nuevo de ADM/REC. |
| RF-28: auditoría global de citas/caja | Hay auditoría en identidad y administración, e historial/compensaciones de caja; falta una consulta global y registro de todas las operaciones exigidas. |
| RF-23: corrección contable sin devolución física | El flujo económico actual cubre devolución completa y anulación de cargo. No distingue una corrección que no implique entregar dinero. |
| RF-22 / RF-27: filtros económicos y agregaciones | Se consulta por fecha/cita y se listan movimientos; faltan búsqueda avanzada por paciente/estado, paginación y resúmenes explícitos por medio. |
| RF-12: agenda semanal y disponibilidad de consulta ADM/PRO | Agenda diaria disponible; el endpoint de propuestas de reserva se limita a REC. |
| RNF-08 y adaptación visual | Faltan recorridos manuales completos por teclado, comparación visual y verificación de navegadores/viewports requerida. Las pruebas Axe existentes son parciales. |
| RNF-12: mutación de reglas críticas | Pruebas funcionales y de concurrencia ejecutadas; no se declara una campaña de mutación. |
| RF-03 y funcionalidades F16 | Restablecimiento de credenciales, reportes operativos ampliados y exportación siguen fuera de esta integración. |

No se amplía automáticamente la implementación de esas funciones en una etapa de validación. Deben convertirse en tareas de aceptación explícitas. El paso 5 del recorrido corresponde a publicar/revisar la rama y decidir su integración en main; el resultado puede revisarse sin afirmar cumplimiento completo de P8.
