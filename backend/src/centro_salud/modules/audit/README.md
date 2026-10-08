# Auditoría de negocio

Referencia: RF-28.

Responsabilidad prevista: Registro y consulta de acciones. No sustituye ni se confunde con telemetría.

- `domain/`: entidades y reglas propias del módulo, sin frameworks.
- `application/use_cases/`: coordinación de operaciones autorizadas.
- `application/ports/`: contratos requeridos para persistencia y colaboraciones externas.

No hay comportamiento implementado. Los adaptadores concretos se ubicarán en `centro_salud/adapters/` y la composición en `bootstrap/`.
