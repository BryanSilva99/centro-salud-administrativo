# Pacientes y recursos administrativos

Módulo del monolito Spring Boot. Implementa las fichas profesionales básicas usadas por el vínculo de RF-02 el registro de pacientes de RF-04 ([docs/PACIENTES.md](../../../../../../../../docs/PACIENTES.md)) y los servicios con modalidades económicas de RF-08 ([docs/SERVICIOS.md](../../../../../../../../docs/SERVICIOS.md)). La búsqueda y la edición de pacientes (RF-05) y la gestión completa de recursos siguen pendientes.

- `domain/`: entidades, estados y reglas en Java sin dependencias de Spring.
- `application/usecases/`: coordinación de operaciones del módulo.
- `application/ports/`: contratos de colaboración y persistencia.

Los adaptadores concretos se ubicarán en `pe.centrosalud.adapters`; la composición de Spring en `pe.centrosalud.bootstrap`.
