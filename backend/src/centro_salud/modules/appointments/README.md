# Citas y atención

Referencia: RF-12 a RF-18.

Responsabilidad prevista: Reserva, reprogramación, llegada y transiciones de atención administrativa.

- `domain/`: entidades y reglas propias del módulo, sin frameworks.
- `application/use_cases/`: coordinación de operaciones autorizadas.
- `application/ports/`: contratos requeridos para persistencia y colaboraciones externas.

No hay comportamiento implementado. Los adaptadores concretos se ubicarán en `centro_salud/adapters/` y la composición en `bootstrap/`.
