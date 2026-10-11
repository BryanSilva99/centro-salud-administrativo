# Integración de administración — paso 1

11 de octubre de 2026. Rama `integration/administracion`, basada en `main` (`0a08f2f`) e integrada con `feature/administracion` (`37fe81c`). Incluye el historial de `feature/pacientes`; no requiere una segunda fusión de pacientes.

## Resultado

Se incorporan alta, búsqueda y edición de pacientes, servicios y modalidades, consultorios, profesionales y turnos. Se conserva la interfaz de identidad de main y se resuelve el conflicto en SessionView incorporando los accesos al menú existente. Solo se muestra un área a la vez. Al revocar su permiso, se desmonta el área y se vuelve al inicio; recuperar el permiso no reabre el formulario.

REC accede a pacientes; ADM a recursos y cuentas conforme los permisos recibidos. ADM+REC reúne ambos accesos. No se añaden permisos implícitos ni dashboards.

## Comprobaciones

- Frontend: `npm test`, 71 pruebas aprobadas; `npm run build`, compilación y tipos correctos.
- Backend: `RUN_POSTGRES_TESTS=true ./mvnw verify`, con esquemas de prueba aislados: 162 pruebas aprobadas, ninguna omitida. Incluye HTTP, PostgreSQL, concurrencia, autorización y Spotless.
- Flyway aplica V1–V14 en los esquemas nuevos de las pruebas. Las migraciones V1–V8 permanecen idénticas a main.
- Comprobación SQL adicional en una transacción temporal: crear V1–V8, insertar profesional y cuenta vinculada sintéticos, aplicar V9–V14 y comprobar que se conservan el vínculo y el profesional con versión inicial 0. Rollback al terminar. Sin números de migración repetidos.
- Regresiones de navegación: alternar pacientes/cuentas, selección activa, retorno al inicio con foco y revocación/recuperación del permiso.

## Límites y siguiente paso

Esta integración no incorpora citas ni caja. Los criterios de disponibilidad combinada, impacto sobre citas y lectura limitada de pacientes para CAJ/PRO siguen pendientes, como detallan PACIENTES.md, PROFESIONALES.md, CONSULTORIOS.md y DISPONIBILIDAD.md.

Se conservan el CSS y los componentes de identidad de main. Las pantallas administrativas importadas no se declaran adaptadas ni verificadas visualmente contra Figma; su adaptación visual es posterior. Los documentos de extracción de ui-ux describen la fotografía anterior a esta integración.

Siguiente paso: integrar citas, ordenar sus migraciones y conectar sus contratos con administración e identidad. La fusión a main se revisa por separado.
