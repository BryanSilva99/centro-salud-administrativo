# Migraciones

`V1__spring_session.sql` contiene el esquema PostgreSQL oficial de Spring Session JDBC 4.1.1 (recurso `org/springframework/session/jdbc/schema-postgresql.sql` del JAR). Prepara únicamente `spring_session` y `spring_session_attributes`; no implementa autenticación ni crea cuentas del sistema.

Flyway aplica y registra las migraciones al arrancar. Hibernate usa `ddl-auto=validate`; Spring Session mantiene `initialize-schema=never` para evitar dos mecanismos de creación de tablas. No editar migraciones ya aplicadas: los cambios posteriores deberán ir en nuevas versiones.

`V2__cuentas.sql` añade la primera tabla de cuentas (ID, identificador único y estado). No precarga cuentas ni credenciales. Los demás componentes se añadirán en pasos posteriores.

`V3__credenciales_cuenta.sql` añade un hash opcional con formato validado. Las cuentas sin hash no pueden superar la verificación. No añade contraseñas ni datos de demostración.

`V4__control_acceso.sql` añade versión de acceso con revocación por cambio de hash/estado y almacenamiento del límite de intentos. No crea cuentas de demostración.

`V5__roles_cuenta.sql` añade roles por cuenta sin asignaciones automáticas. Sus cambios se leen en cada solicitud; no se confía en los roles antiguos guardados en la sesión.

V5–V6 añaden roles, versión de edición, bloqueo y auditoría de roles. V7 añade auditoría de cuentas e incrementa la versión de edición al cambiar identificador o estado. V8 añade fichas profesionales y el vínculo con cuentas. No se modifican migraciones aplicadas.

`V9__pacientes.sql` añade pacientes con documento único (tipo + número), un código interno `P` + 8 dígitos generado por secuencia (puede tener huecos si un alta se revierte) y la auditoría de altas sin datos personales.

`V10__servicios.sql` añade servicios (nombre único sin distinguir mayúsculas, duración de 1 a 480 minutos y versión de edición), sus modalidades económicas (tarifa `NUMERIC(10,2)` no negativa; con cargo exige tarifa mayor que cero y sin cargo, tarifa cero) y la auditoría con la foto JSON anterior y nueva. No hay borrado físico: se desactivan.
