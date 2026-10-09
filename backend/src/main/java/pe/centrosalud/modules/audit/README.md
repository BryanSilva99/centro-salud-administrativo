# Auditoría de negocio

Módulo del monolito Spring Boot. Sin clases ni comportamiento implementados.

- `domain/`: entidades, estados y reglas en Java sin dependencias de Spring.
- `application/usecases/`: coordinación de operaciones del módulo.
- `application/ports/`: contratos de colaboración y persistencia.

Los adaptadores concretos se ubicarán en `pe.centrosalud.adapters`; la composición de Spring en `pe.centrosalud.bootstrap`.
