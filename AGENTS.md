# Límites de trabajo

Estado autorizado: estructura y preparación técnica del proyecto. El 9 de octubre el usuario autorizó configurar e instalar dependencias y verificar la compilación. También autorizó configurar PostgreSQL local e iniciar el entorno para comprobar la conexión. Además autorizó implementar la primera parte de Cuenta en todas las capas hasta el controlador: consulta por ID, persistencia y pruebas. También autorizó el siguiente paso: búsqueda por identificador, hash y verificación de credenciales con cuentas temporales de prueba. El usuario autorizó continuar con el inicio/cierre de sesión, consulta de sesión, límite de intentos y pruebas. El usuario autorizó continuar con el formulario React conectado al login, una cuenta local de prueba y abrir IntelliJ IDEA al terminar. La gestión completa de cuentas, roles y otros módulos quedan para pasos posteriores.

Stack confirmado por el usuario: Java con Spring Boot para backend y React para frontend. No usar Python para el backend.

- No implementar funcionalidades, reglas, endpoints, pantallas, persistencia, pruebas ejecutables ni despliegue hasta que el usuario lo solicite expresamente.
- Una fecha de entrega o contexto adicional no autoriza iniciar implementación.
- Se permiten dependencias, puntos de arranque y configuración técnica. No iniciar servidores persistentes ni agregar funciones de negocio sin indicación expresa.
- Leer docs/ORGANIZACION_DEL_CODIGO.md y los requisitos antes de proponer cambios.
- Mantener dominio independiente de frameworks y adaptadores.
- No incorporar respaldos, credenciales, datos de pacientes, pruebas anteriores o trabajos de Tarea2.
- Documentar el estado real: carpetas preparadas no equivalen a requisitos implementados.
