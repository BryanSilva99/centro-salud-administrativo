# Pacientes y recursos administrativos

Módulo del monolito Spring Boot. Implementa el registro, búsqueda y edición de pacientes de RF-04 y RF-05 ([docs/PACIENTES.md](../../../../../../../../docs/PACIENTES.md); la lectura mínima de caja y de profesionales de CA-3 depende de caja y citas), los servicios con modalidades económicas de RF-08 ([docs/SERVICIOS.md](../../../../../../../../docs/SERVICIOS.md)), los consultorios de RF-09 ([docs/CONSULTORIOS.md](../../../../../../../../docs/CONSULTORIOS.md)), los profesionales completos de RF-07, que extienden la ficha básica usada por el vínculo de RF-02 ([docs/PROFESIONALES.md](../../../../../../../../docs/PROFESIONALES.md)), y los turnos de disponibilidad de RF-10 ([docs/DISPONIBILIDAD.md](../../../../../../../../docs/DISPONIBILIDAD.md)), aunque la consulta combinada de disponibilidad depende de RF-13.

- `domain/`: entidades, estados y reglas en Java sin dependencias de Spring.
- `application/usecases/`: coordinación de operaciones del módulo.
- `application/ports/`: contratos de colaboración y persistencia.

Los adaptadores concretos se ubicarán en `pe.centrosalud.adapters`; la composición de Spring en `pe.centrosalud.bootstrap`.
