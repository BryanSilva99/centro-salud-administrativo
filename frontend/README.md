# Frontend

Base React, Vite y TypeScript preparada. La pantalla autenticada ofrece agenda diaria, horarios disponibles, programación, reprogramación, llegada, sala de espera, atención, cancelación, ausencia, retiro de espera y dashboard operativo. Vista semanal, reaperturas, caja y reportes detallados continúan pendientes. Ver [guía de preparación](../docs/INICIO_DESARROLLO.md).

- `src/app/`: futura composición, navegación y proveedores.
- `src/features/`: organización por área funcional.
- `src/shared/ui/`: componentes visuales reutilizables.
- `src/shared/api/`: futuro cliente HTTP común.
- `src/shared/lib/`: utilidades de presentación.
- `tests/`: verificaciones futuras de accesibilidad y recorridos.

La web presenta información y solicita operaciones. La autorización y las reglas definitivas pertenecen al backend; el cliente no calcula duración, importe ni disponibilidad definitiva.
