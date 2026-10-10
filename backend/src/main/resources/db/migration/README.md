# Migraciones

`V1__spring_session.sql` contiene el esquema PostgreSQL oficial de Spring Session JDBC 4.1.1 (recurso `org/springframework/session/jdbc/schema-postgresql.sql` del JAR). Prepara únicamente `spring_session` y `spring_session_attributes`; no implementa autenticación ni crea cuentas del sistema.

Flyway aplica y registra las migraciones al arrancar. Hibernate usa `ddl-auto=validate`; Spring Session mantiene `initialize-schema=never` para evitar dos mecanismos de creación de tablas. No editar migraciones ya aplicadas: los cambios posteriores deberán ir en nuevas versiones.

`V2__cuentas.sql` añade la primera tabla de cuentas (ID, identificador único y estado). No precarga cuentas ni credenciales. Los demás componentes se añadirán en pasos posteriores.

`V3__credenciales_cuenta.sql` añade un hash opcional con formato validado. Las cuentas sin hash no pueden superar la verificación. No añade contraseñas ni datos de demostración.

`V4__control_acceso.sql` añade versión de acceso con revocación por cambio de hash/estado y almacenamiento del límite de intentos. No crea cuentas de demostración.
