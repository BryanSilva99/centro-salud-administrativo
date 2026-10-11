# Preparación del entorno de desarrollo

Actualizado el 9 de octubre de 2026. Se prepararon dependencias y puntos de arranque. Existe una primera consulta de Cuenta por ID, con acceso HTTP cerrado; ver [recorrido y pruebas](CUENTA_PASO_1.md). Login HTTP, consulta de sesión y logout están implementados; la gestión completa de usuarios sigue pendiente. React ya dispone de formulario de acceso y vista de sesión; ver [paso 4](LOGIN_PASO_4_REACT.md). Ver [paso 3](LOGIN_PASO_3.md).

## Herramientas

- Java 21.
- Maven 3.9.16 mediante el wrapper incluido: `mvnw` en Linux/macOS y `mvnw.cmd` en Windows. No hace falta instalar Maven globalmente.
- Node.js 24 LTS recomendado, indicado en `frontend/.nvmrc`.
- PostgreSQL 18.4, instalado localmente o mediante Docker Compose.
- IntelliJ IDEA: abrir `backend/pom.xml` como proyecto Maven o incorporarlo al proyecto abierto mediante «Add as Maven Project»; elegir JDK 21.

## Dependencias del backend

Spring Boot 4.1.1 administra las versiones compatibles mediante su POM padre.

| Dependencia | Para qué se prepara |
|---|---|
| Spring Web MVC | Futuros controladores HTTP |
| Validation | Validación de solicitudes |
| Spring Security | Mecanismos de seguridad para implementar el acceso |
| Spring Data JPA | Adaptadores de persistencia; no se usa desde el dominio |
| PostgreSQL JDBC | Conexión a PostgreSQL |
| Flyway y soporte PostgreSQL | Migraciones versionadas |
| Spring Session JDBC | Persistencia futura de sesiones |
| Starters de pruebas | Herramientas de pruebas para las tecnologías seleccionadas |

`CentroSaludApplication` solo inicia Spring. `bootstrap/SecurityConfiguration` habilita solo autenticación, consulta de sesión y actividad; la consulta de cuentas exige el permiso de ADM y las demás rutas de negocio siguen cerradas. No habilita un formulario automático ni HTTP Basic. El logout es POST con CSRF. La protección CSRF permanece activa. Spring puede emitir su credencial automática de desarrollo al arrancar: no se usa como cuenta del sistema ni habilita acceso con esta configuración.

La migración V2 añade cuentas y existe un adaptador JPA de consulta. V3 añade almacenamiento de hash y existe verificación interna de credenciales. No hay cuentas precargadas por migraciones; V5 almacena roles y se aplican en cada solicitud; ver [paso 2](CREDENCIALES_PASO_2.md). La migración V1 crea únicamente las dos tablas técnicas de Spring Session, usando el esquema oficial de Spring Session JDBC 4.1.1. Flyway registra su aplicación; esto no implementa el login. La expiración absoluta de ocho horas, inactividad de 30 minutos y limitación de intentos están implementadas.

## Preparar PostgreSQL

En este equipo se configuró PostgreSQL 18.4 local sin instalar un servicio del sistema. Desde la raíz del repositorio:

```bash
cd infra/local
npm ci
npm run db
```

Mantener esa terminal abierta; Ctrl+C detiene la base y conserva sus datos. El script crea una base nueva solo la primera vez y reutiliza la existente al reiniciar. Usa `127.0.0.1:5432`, base y usuario `centro_salud`. Genera `backend/.env` con contraseña aleatoria solo si no existe. La cuenta de la aplicación no es superusuario. El administrador local se guarda aparte en `.data/postgres-admin.json`.

Los datos están en `.data/postgres`; esa carpeta y las credenciales se excluyen de Git. No borrar `.data` para reiniciar. Para conectar desde la ventana Database de IntelliJ: PostgreSQL, host `127.0.0.1`, puerto `5432`, base y usuario `centro_salud`, contraseña indicada en `backend/.env`. No confundir esta cuenta de base de datos con un usuario del futuro login.

Se verificó esta herramienta en Linux x64. npm 12 tiene autorizado en `package.json` el script de instalación del paquete nativo de esta plataforma. Para otros sistemas puede usarse PostgreSQL instalado o Docker, según la alternativa siguiente. No iniciar dos bases en el mismo puerto.

### Alternativa: PostgreSQL instalado o Docker

Copiar `backend/.env.example` a `backend/.env` y asignar una contraseña local a `DB_PASSWORD`. No subir ese archivo a Git. El backend lee ese archivo cuando se ejecuta desde `backend/`.

Si ya se dispone de PostgreSQL, crear la base `centro_salud` y el usuario y ajustar `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. Como alternativa, desde la raíz del repositorio y con Docker disponible:

```bash
docker compose --env-file backend/.env -f infra/local/compose.yml up -d
```

El puerto 5432 debe estar libre. No se inicia esta base automáticamente al compilar. El volumen conserva los datos; no eliminarlo para solucionar problemas de arranque.

## Compilar e iniciar backend

Desde `backend/`:

```bash
./mvnw package -DskipTests
./mvnw spring-boot:run
```

En Windows usar `mvnw.cmd` en lugar de `./mvnw`. El arranque requiere PostgreSQL y las variables configuradas. Compilar no necesita conexión a una base.

El servidor usa el puerto 8080. Existen `/api/auth/login`, `/api/auth/me` y `/api/auth/logout`, además de `/api/auth/csrf` y `/api/auth/activity`. Contratos en la guía del paso 3; visitar la raíz del servidor no muestra una pantalla.

## Preparar e iniciar frontend

Desde `frontend/`:

```bash
npm ci
npm run build
npm run dev
```

El servidor de desarrollo usa `http://127.0.0.1:5173`. React muestra el formulario de acceso, recupera la sesión al recargar y permite cerrarla. La cuenta local se prepara siguiendo el paso 4. Las futuras solicitudes relativas a `/api` se enviarán al backend mediante el proxy de Vite.

Se puede copiar `.env.example` a `.env` para cambiar `API_PROXY_TARGET`. No colocar secretos en variables `VITE_*`: son públicas en el navegador.

React 19.3.0, Vite 8.3.4 y TypeScript 7.0.2 están fijados en `package.json`; `package-lock.json` conserva la resolución completa. Se incluyen Vitest, Testing Library, jsdom y Axe-core como herramientas de desarrollo. Hay 10 pruebas del login y análisis Axe sobre formulario y error en jsdom (sin evaluación de contraste). `npm test` exige pruebas presentes.

## Límites de esta preparación

- Se implementa autenticación por sesión y su límite de intentos. No hay gestión de cuentas/roles ni otros procesos del negocio.
- No se despliega la aplicación ni se integra SigNoz todavía.
- PIT y la instrumentación se configurarán al desarrollar las primeras reglas y recorridos verificables; no se atribuye su uso a esta preparación.
- Verificado: PostgreSQL 18.4, cuenta sin superusuario, lectura/escritura temporal con rollback, migración V1 y arranque de Spring Boot conectado a la base. React contiene el login conectado. La API de autenticación se verificó con HTTP real; las rutas protegidas devuelven 401 sin sesión y las rutas de negocio denegadas devuelven 403 con sesión. Esto verifica el entorno, no funcionalidades. Docker sigue siendo una alternativa no probada en este equipo.

Fuentes técnicas: [Spring Initializr](https://start.spring.io/), [requisitos de Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html) y [guía de Vite](https://vite.dev/guide/).

Contrato final de sesión, roles y pruebas: [Login completo](LOGIN_COMPLETO.md).
