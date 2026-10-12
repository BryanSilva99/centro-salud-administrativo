# Integración de citas — paso 2

> Estado histórico de este paso. La [integración de caja](INTEGRACION_CAJA.md) reemplaza el aviso económico conservador por la consulta de cargos reales.

11 de octubre de 2026. Rama `integration/citas`, sobre administración integrada (`4b2ccca`), incorporando `feat/appointments-module` (`09249fe`). Main y las ramas remotas no se modifican.

## Resultado y decisiones

- Se conserva el contenedor, navegación, sesión y cuentas de main. Agenda y citas se abre como otra área; los estilos importados quedan limitados al componente. No se declara adaptación visual a Figma.
- Catálogo y disponibilidad consultan puertos públicos de administración, sin SQL sobre tablas de otros módulos. Los pacientes completos no se exponen en el contexto de ADM/PRO; REC recibe identificación mínima para reservar. El catálogo de profesionales no se trunca a los 50 resultados del buscador de fichas.
- Las capacidades provienen de los roles actuales y del vínculo profesional real. REC reserva y gestiona admisión/cierres; ADM consulta agenda, espera y conteos; PRO vinculado a una ficha activa solo consulta su agenda y atiende sus citas. La ficha no se asigna automáticamente. Una cuenta con varios roles conserva el límite de atención al profesional asignado.
- La disponibilidad exige servicio activo, profesional habilitado y consultorio compatible. Intersecta turnos DISPONIBLE activos de ambos recursos, une intervalos contiguos y descuenta reservas. Las propuestas se alinean a cuartos de hora aun cuando un turno empiece fuera de esa cuadrícula.
- V15–V17 incorporan reservas, reprogramación, llegada, atención y cierres. V1–V14 permanecen idénticas. Se reemplaza la dependencia de btree_gist de la rama original por un trigger con bloqueos transaccionales ordenados por recurso, que rechaza solapes en PostgreSQL sin extensiones ni superusuario. Los índices únicos de atención activa se conservan.
- Las fixtures de citas solo existen en `src/test`; la variable de demostración no puede activar permisos ficticios en ejecución.
- Hasta integrar caja, cancelar o retirar una cita con importe muestra revisión económica pendiente. No crea cargos, no consulta saldos ni afirma que se haya conciliado el dinero.

## Comprobaciones

- Backend: `./mvnw verify` con PostgreSQL habilitado: **188 pruebas aprobadas, ninguna omitida**; incluye Spotless y empaquetado.
- Frontend: **81 pruebas aprobadas**, comprobación TypeScript y compilación de producción correctas.
- Base local: Flyway validó 17 migraciones y aplicó V15–V17 sobre V14 al reiniciar el backend.
- Navegador: login con la cuenta local y apertura de Agenda y citas, conservando el menú de administración. Los catálogos vacíos reflejan que todavía no hay recursos de prueba en esa base.

 Las pruebas PostgreSQL usan esquemas aislados y `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=4`, `SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=1` para que los contextos Spring no agoten las conexiones locales.

Se añaden recorridos HTTP con login y CSRF reales: reserva, reprogramación con versión, llegada, atención por el profesional asignado, consulta por rol, revocación, catálogos sin pacientes para ADM/PRO, rechazo sin CSRF, recursos inactivos o incompatibles, modalidades inactivas, alineación de horarios, cambios de disponibilidad y retiro. Seis reservas simultáneas del mismo horario confirman una sola; cancelar libera el horario.

## Cómo probar

1. Entrar con REC o ADM+REC y registrar un paciente.
2. Con ADM, crear un servicio activo y su modalidad, un profesional habilitado para ese servicio y un consultorio compatible.
3. Configurar turnos DISPONIBLE de ambos recursos en una fecha/hora futura que coincida.
4. Abrir Agenda y citas con REC, elegir recursos, modalidad y horario y reservar.
5. Para atención, vincular una cuenta PRO a la ficha profesional desde Usuarios y roles. REC registra la llegada en la fecha de la cita; esa cuenta PRO inicia y finaliza la atención.

## Límites pendientes

- Caja se integra en el paso 3; el aviso económico es conservador y no es un cálculo de saldo.
- RF-11 y RN-08: aún falta el flujo administrativo que muestra y resuelve citas afectadas antes de desactivar recursos o turnos. El inicio de atención ya rechaza una cita cuyo horario o recursos dejaron de estar habilitados.
- Vista semanal, auditoría global y adaptación visual detallada no se completan aquí.
- Catálogos de selección cargados completos: para volúmenes grandes conviene un buscador paginado de reservas; el directorio de pacientes existente mantiene su paginación.
- Bases de demostración que ya aplicaron las antiguas V5–V7 de citas necesitan una transición específica. No usar repair ni renumerar su historial a ciegas. La base local de administración continúa desde V14.
