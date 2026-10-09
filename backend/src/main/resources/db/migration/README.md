# Migraciones futuras

Flyway está preparado, pero aún no hay migraciones SQL. Las tablas de cuentas,
sesiones y negocio se crearán cuando se autorice implementar esos componentes.
`ddl-auto=validate` impide que Hibernate genere el modelo silenciosamente.
Las tablas de Spring Session también requerirán una migración antes del login.
