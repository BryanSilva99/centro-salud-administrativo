# Identidad y acceso

Referencia: RF-01 a RF-03.

Responsabilidad prevista: Cuentas, roles, sesiones y políticas por acción y recurso.

- `domain/`: entidades y reglas propias del módulo, sin frameworks.
- `application/use_cases/`: coordinación de operaciones autorizadas.
- `application/ports/`: contratos requeridos para persistencia y colaboraciones externas.

No hay comportamiento implementado. Los adaptadores concretos se ubicarán en `centro_salud/adapters/` y la composición en `bootstrap/`.
