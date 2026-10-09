# Preparación del entorno de desarrollo

Actualizado el 9 de octubre de 2026. Se prepararon dependencias y puntos de arranque; no hay funcionalidades de negocio, login, usuarios ni endpoints implementados.

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

`CentroSaludApplication` solo inicia Spring. `bootstrap/SecurityConfiguration` bloquea todas las solicitudes hasta implementar explícitamente autenticación. No habilita un formulario de login, HTTP Basic ni un endpoint de logout. La protección CSRF permanece activa. Spring puede emitir su credencial automática de desarrollo al arrancar: no se usa como cuenta del sistema ni habilita acceso con esta configuración.

No hay usuarios, repositorios, entidades JPA ni migraciones de tablas de negocio. Las tablas de Spring Session se deberán crear mediante migración cuando se implemente autenticación; no se inicializan silenciosamente. La expiración absoluta de ocho horas y los demás requisitos del login siguen pendientes.

## Preparar PostgreSQL

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

El servidor usa el puerto 8080. Todavía no hay `/api/auth/login`, `/api/auth/me` ni `/api/auth/logout`; una respuesta de acceso denegado al visitar el servidor es coherente con el cierre preventivo, no una pantalla implementada.

## Preparar e iniciar frontend

Desde `frontend/`:

```bash
npm ci
npm run build
npm run dev
```

El servidor de desarrollo usa `http://127.0.0.1:5173`. La entrada React está vacía intencionalmente: no se ha diseñado una pantalla ni un login. Las futuras solicitudes relativas a `/api` se enviarán al backend mediante el proxy de Vite.

Se puede copiar `.env.example` a `.env` para cambiar `API_PROXY_TARGET`. No colocar secretos en variables `VITE_*`: son públicas en el navegador.

React 19.3.0, Vite 8.3.4 y TypeScript 7.0.2 están fijados en `package.json`; `package-lock.json` conserva la resolución completa. Se incluyen Vitest, Testing Library, jsdom y Axe-core como herramientas de desarrollo. No se han creado pruebas funcionales ni activado un análisis de accesibilidad sobre pantallas inexistentes. `npm test` permite por ahora que no haya pruebas; su éxito no acredita funcionalidades.

## Límites de esta preparación

- No se implementa autenticación ni ninguna regla del negocio.
- No se despliega la aplicación ni se integra SigNoz todavía.
- PIT y la instrumentación se configurarán al desarrollar las primeras reglas y recorridos verificables; no se atribuye su uso a esta preparación.
- Docker no estaba disponible en el equipo durante la preparación. No se instaló ni se inició PostgreSQL. La configuración de conexión queda preparada; no se afirma haber verificado el arranque integrado con base de datos.

Fuentes técnicas: [Spring Initializr](https://start.spring.io/), [requisitos de Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html) y [guía de Vite](https://vite.dev/guide/).
