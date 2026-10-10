# Identidad y acceso

Módulo del monolito Spring Boot. Primera consulta de Cuenta por ID implementada; verificación interna de credenciales implementada. Sesiones persistidas, login HTTP, logout y control de intentos implementados. Administración de cuentas y roles pendientes.

- `domain/`: entidades, estados y reglas en Java sin dependencias de Spring.
- `application/usecases/`: coordinación de operaciones del módulo.
- `application/ports/`: contratos de colaboración y persistencia.

Los adaptadores concretos se ubicarán en `pe.centrosalud.adapters`; la composición de Spring en `pe.centrosalud.bootstrap`.
