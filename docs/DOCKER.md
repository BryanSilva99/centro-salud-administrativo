# Ejecutar el sistema con Docker

Entorno local para probar el avance integrado: React compilado servido por Nginx, Spring Boot con Java 21 y PostgreSQL 18.4. Solo se publica la web en `127.0.0.1`; API y base de datos quedan en la red de Compose. Nginx dirige `/api` al backend bajo el mismo origen, conservando sesiones y CSRF.

Se requiere Docker Engine con Compose v2.20 o posterior, o Docker Desktop con Compose. No hace falta instalar Java, Maven, Node ni PostgreSQL en el equipo. La primera construcción requiere Internet para descargar imágenes y dependencias. Usar el repositorio `sistema-salud` como directorio de trabajo.

## 1. Configurar

Copiar el archivo de ejemplo de la **raíz** del repositorio:

```sh
cp .env.example .env
```

En PowerShell:

```powershell
Copy-Item .env.example .env
```

Editar `.env` y completar `DB_ADMIN_PASSWORD`, `DB_PASSWORD` y `ADMIN_PASSWORD` con contraseñas distintas. Usar valores aleatorios de al menos 16 caracteres; la cuenta de aplicación admite entre 12 y 128. Para símbolos especiales, utilizar la sintaxis de comillas de los archivos `.env` de Compose. No publicar este archivo ni compartirlo con el repositorio.

- `DB_ADMIN_PASSWORD`: inicialización administrativa de PostgreSQL.
- `DB_PASSWORD`: conexión del backend con el usuario `centro_salud`, sin privilegios de superusuario.
- `ADMIN_IDENTIFIER`: cuenta ADM inicial de la aplicación, por defecto `admin.local`.
- `ADMIN_PASSWORD`: contraseña de esa cuenta, usada únicamente al ejecutar el aprovisionamiento explícito.
- `WEB_PORT`: puerto local de la web, por defecto `8088`.

Este `.env` es independiente de `backend/.env`, utilizado por el arranque sin Docker. Las imágenes no incluyen ninguno de los dos archivos ni `.data`, credenciales o dependencias locales.

## 2. Construir y arrancar

```sh
docker compose config --quiet
docker compose up --build -d --wait
```

La base tiene un healthcheck. El backend espera a que esté disponible y aplica Flyway; Nginx espera al backend saludable. Las imágenes compilan desde el código y los archivos de dependencias fijadas. No se utiliza el servidor de desarrollo Vite para servir la web.

La aplicación se abre en **http://localhost:8088/**, o el puerto configurado en `WEB_PORT`. Usar `localhost` para Docker y `127.0.0.1:5173` para el entorno Vite existente permite mantener separadas las cookies en el navegador.

## 3. Crear el administrador inicial

Después de arrancar, ejecutar:

```sh
docker compose --profile tools run --rm --build bootstrap
```

Entrar con `ADMIN_IDENTIFIER` y `ADMIN_PASSWORD` del `.env`. El comando crea únicamente el primer ADM y su registro de auditoría. Un reintento verifica la cuenta existente y no cambia su contraseña, estado ni roles. Si la configuración no coincide o ya existe otro ADM activo, rechaza el aprovisionamiento; administrar las demás cuentas desde la aplicación.

Desde **Usuarios y roles**, el ADM puede crear cuentas REC, CAJ y PRO. Para atender, vincular la cuenta PRO a su ficha profesional. Crear servicios, profesionales, consultorios y turnos desde administración; registrar pacientes y citas con REC; cobrar con CAJ. Usar otro ADM para aprobar una reversión solicitada por CAJ.

La base Docker empieza vacía: las cuentas como `bryan.local` y los datos de `.data/postgres` del entorno anterior no se copian ni se sobrescriben. Cada compañero crea sus propias credenciales y datos ficticios.

## 4. Revisar y detener

```sh
docker compose ps
docker compose logs --tail=100 backend
docker compose logs --tail=100 frontend
docker compose logs --tail=100 postgres
docker compose down
```

`down` conserva el volumen de PostgreSQL. Para actualizar después de cambiar de versión del código, repetir `docker compose up --build -d --wait`; Flyway aplica las migraciones pendientes sin cambiar las ya ejecutadas.

No usar `down -v` para reiniciar: esa opción elimina el volumen y sus datos. Cambiar las contraseñas de base en `.env` tampoco modifica usuarios de un volumen ya inicializado; mantener la configuración original o realizar una rotación explícita fuera de este procedimiento.

Si el puerto está ocupado, cambiar `WEB_PORT`. Si falta Compose o no hay acceso al motor, revisar la instalación de Docker del equipo; ese error no se resuelve modificando contraseñas de la aplicación. En Linux puede ser necesario ejecutar los comandos con `sudo`, según la configuración local.

## Alcance

Este despliegue es para pruebas locales por HTTP: `SESSION_COOKIE_SECURE=false` se fija solo en Compose. Para publicar en Internet se necesita configurar HTTPS, cookies seguras, secretos y operación del despliegue. SigNoz y pruebas de mutación siguen fuera de este trabajo; Docker no sustituye esas tendencias.

## Verificación

12 de octubre de 2026, Linux x86_64, Docker 29.9.0 y Compose 5.6.0:

- Construcción real de las imágenes backend, frontend y bootstrap desde sus Dockerfiles y dependencias fijadas.
- Base nueva con 18 migraciones Flyway; conexión del backend con usuario sin superusuario.
- Tres servicios saludables; solo la web publicada en loopback. Reinicio del backend con recuperación del proxy.
- Creación del ADM inicial, reintento sin duplicación y rechazo de otra contraseña sin restablecer la cuenta.
- HTTP a través de Nginx: archivos compilados y fuente Inter local verificada por hash, login/cookies/CSRF, permisos por rol, alta de recursos y paciente, reserva, cargo y cobro, cancelación con gestión económica pendiente, devolución aprobada por ADM y cierre conciliado.
- `down` y posterior `up` sin borrar volumen conservan cuenta, sesión autenticada, cargos, movimientos y cierre.
- Sintaxis de Compose, shell y Node validada. Credenciales y datos locales excluidos de Git y de los contextos de construcción.

La prueba se ejecutó en un proyecto Compose aislado y se retiró su volumen temporal al terminar. Se dejó el proyecto local `centro-salud` listo en el puerto 8088 con un ADM nuevo, sin copiar datos del entorno previo. No se ejecutaron pruebas nativas en Windows/macOS o ARM; se fijan finales LF para scripts y las imágenes base no fuerzan arquitectura.

Referencias de implementación: [construcciones por etapas](https://docs.docker.com/build/building/multi-stage/), [dependencias saludables](https://docs.docker.com/compose/how-tos/startup-order/) e [imagen oficial PostgreSQL](https://hub.docker.com/_/postgres).
