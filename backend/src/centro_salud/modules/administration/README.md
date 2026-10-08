# Gestión administrativa

Referencia: RF-04 a RF-11.

Responsabilidad prevista: Pacientes, profesionales, servicios, consultorios, disponibilidad y bloqueos.

- `domain/`: entidades y reglas propias del módulo, sin frameworks.
- `application/use_cases/`: coordinación de operaciones autorizadas.
- `application/ports/`: contratos requeridos para persistencia y colaboraciones externas.

No hay comportamiento implementado. Los adaptadores concretos se ubicarán en `centro_salud/adapters/` y la composición en `bootstrap/`.
