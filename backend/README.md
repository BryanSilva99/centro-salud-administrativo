# Backend

Estructura prevista de un monolito modular con arquitectura hexagonal. Los archivos Python contienen únicamente documentación de paquetes: no hay funciones, clases, rutas ni inicialización de servicios.

- `src/centro_salud/modules/`: módulos del negocio y casos de uso.
- `src/centro_salud/adapters/`: entradas HTTP y conexiones externas.
- `src/centro_salud/bootstrap/`: futura composición de dependencias.
- `tests/`: ubicación prevista de verificaciones, sin pruebas implementadas.

FastAPI pertenecerá al adaptador HTTP; PostgreSQL al adaptador de persistencia. Ninguno debe importarse desde el dominio.
