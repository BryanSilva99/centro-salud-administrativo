# Reportes

Referencia: RF-25 a RF-27 y RF-29.

Responsabilidad prevista: Consultas e indicadores autorizados. RF-26 y RF-29 corresponden a la entrega final.

- `domain/`: entidades y reglas propias del módulo, sin frameworks.
- `application/use_cases/`: coordinación de operaciones autorizadas.
- `application/ports/`: contratos requeridos para persistencia y colaboraciones externas.

No hay comportamiento implementado. Los adaptadores concretos se ubicarán en `centro_salud/adapters/` y la composición en `bootstrap/`.
