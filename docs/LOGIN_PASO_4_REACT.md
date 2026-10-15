# Login React conectado al backend

Se implementó el formulario de acceso y el workspace autenticado de appointments con agenda diaria, programación, reprogramación, llegada, espera, atención y dashboard operativo con catálogo sintético opt-in.

## Archivos para revisar en IntelliJ IDEA

| Archivo | Responsabilidad | Por qué se separa |
|---|---|---|
| `frontend/src/app/App.tsx` | Decide entre comprobar sesión, mostrar formulario, mostrar cuenta autenticada o permitir reintentar conexión. | Compone la aplicación sin mezclar solicitudes HTTP con cada elemento visual. |
| `frontend/src/features/identity/components/LoginForm.tsx` | Campos, mostrar/ocultar contraseña, envío, errores y estado de espera. | Mantiene juntos los comportamientos del formulario; no decide permisos. |
| `frontend/src/features/identity/components/SessionView.tsx` | Vista mínima de identidad conservada como componente reutilizable. | El flujo autenticado principal usa ahora `AppointmentsWorkspace`. |
| `frontend/src/features/identity/api/auth.ts` | Llama a login, sesión, logout y actividad; obtiene CSRF antes de cada escritura. | Conserva el contrato del módulo de identidad en un solo lugar. |
| `frontend/src/shared/api/http.ts` | Cookies del mismo origen, tiempo máximo de espera y errores comprensibles. | Evita repetir manejo de conexión; no muestra trazas o cuerpos arbitrarios del servidor. |
| `frontend/src/features/identity/hooks/useSessionActivity.ts` | Comprueba vigencia cada minuto con la página visible y al volver a la pestaña. Informa actividad solo tras eventos reales de teclado/puntero, limitada a una vez por minuto. | La consulta automática no renueva los tiempos de actividad definidos por el backend. |
| `frontend/src/features/appointments/components/AppointmentsWorkspace.tsx` | Agenda diaria, filtros, disponibilidad, programación, reprogramación, llegada, sala de espera, atención, cancelación, ausencia, retiro y dashboard; conserva la sesión y traduce errores de negocio. | No sustituye la autorización del backend ni implementa vista semanal, reaperturas, caja o reportes detallados. |
| `frontend/src/app/styles.css` | Diseño adaptable, estados de foco, contraste visual y presentación. | Separa apariencia y comportamiento. |
| `frontend/tests/login.test.tsx` | Prueba comportamiento del usuario, llamadas a la API y análisis Axe de formulario/error. | Verifica resultados observables, no solo que los componentes rendericen. |
| `infra/local/crear-cuenta.mjs` | Crea expresamente una cuenta local sin permisos de negocio. | Evita insertar usuarios o contraseñas fijas en migraciones y no expone una ruta pública de registro. |

Separar presentación, acceso HTTP y coordinación es una práctica general. Estas carpetas siguen la organización elegida para este proyecto. React no replica la autorización del backend.

## Cómo funciona

1. Al abrir o recargar, React consulta `/api/auth/me`. Un 401 muestra el formulario; un error de red muestra un estado de conexión con reintento.
2. Al enviar, obtiene un token desde `/api/auth/csrf` y manda identificador y contraseña al login. No modifica mayúsculas ni espacios de las credenciales.
3. El navegador recibe y conserva la cookie HttpOnly. React no almacena sesión, contraseña ni tokens en localStorage o sessionStorage.
4. La contraseña se limpia tras éxito o error. El formulario impide envíos repetidos mientras espera; no hay reintentos automáticos del login que consuman el límite de intentos.
5. Tras autenticar se muestra la cuenta y el botón de cierre. La página se restaura al recargar mediante la cookie, no mediante un estado local persistido.
6. Cada escritura obtiene el CSRF vigente. Al cerrar, se llama al backend y después se muestra el formulario. Si falla la conexión, se mantiene la vista y se permite reintentar; no se afirma haber cerrado una sesión que el servidor no confirmó.
7. Una sesión vencida o invalidada devuelve a la pantalla de acceso. Si otra pestaña cambia de cuenta, se abandona la vista de la identidad anterior.

Los 30 minutos/8 horas y el bloqueo por cinco intentos siguen siendo responsabilidad del backend. La actividad explícita responde a RN-02; no se envía por temporizadores ni eventos sintéticos.

## Cuenta local

Desde `infra/local/`, con PostgreSQL iniciado y migraciones aplicadas por el backend:

```bash
npm run cuenta
```

Identificador: `bryan.local`. La contraseña aleatoria se guarda en `.data/cuenta-local.json`, con permisos de lectura/escritura solo para el propietario. Esa carpeta está excluida de Git. No compartir ni subir este archivo.

La herramienta usa el mismo formato PBKDF2 de la aplicación y se comprobó que Spring acepta la credencial creada. Solo admite la base y el usuario indicados en `backend/.env` cuando la conexión es local (`localhost`/`127.0.0.1:5432`); esto permite usar también una base local de pruebas como `centro_salud_test` sin abrir conexiones externas. Se puede volver a ejecutar: no duplica cuentas ni restablece una contraseña existente. Si los datos existentes no coinciden, se detiene. No crea roles ni convierte la cuenta en administrador. Es una herramienta de desarrollo y no sustituye la futura gestión de cuentas y su auditoría.

## Ejecución local

Mantener cada servidor en su terminal:

```bash
# Terminal 1, desde infra/local/
npm run db

# Terminal 2, desde backend/
./mvnw spring-boot:run

# Una vez, desde infra/local/, después del arranque del backend
npm run cuenta

# Terminal 3, desde frontend/
npm run dev
```

Abrir `http://127.0.0.1:5173`. Vite envía `/api` a Spring Boot en el puerto 8080; las llamadas y cookies tienen el mismo origen desde el navegador. Esto es una configuración de desarrollo, no un despliegue de producción.

## Verificación

- 15 pruebas de React aprobadas: login/logout con renovación CSRF, errores y limpieza de contraseña, restauración de sesión, conexión y reintento, invalidación externa, actividad, agenda, programación, reprogramación, sala de espera, inicio de atención, dashboard y accesibilidad automatizada del acceso y del workspace.
- TypeScript y compilación de producción aprobados.
- Con la cuenta local, recorrido real por el proxy de React: login 200, consulta propia 200, logout 204 y consulta posterior 401.
- Revisión visual de escritorio y vista estrecha; sin desbordamiento horizontal en la vista móvil observada. El error de credenciales fue comprobado desde el navegador.
- Axe-core no encontró infracciones en formulario ni error dentro de jsdom. Se desactivó el análisis de contraste por las limitaciones de ese entorno; esto no acredita una auditoría completa de accesibilidad ni todos los flujos de RNF-08.
- Las 37 pruebas del backend habían pasado en el paso anterior; en este paso no se modificó su implementación.

Desde `frontend/`: `npm test` y `npm run build`. Las pruebas usan respuestas HTTP simuladas; el recorrido contra el backend real se verificó por separado. No se afirma haber automatizado un navegador completo para el recorrido exitoso de React.

## Pendiente

Gestión y asignación de roles, permisos por recurso, recuperación de contraseña, auditoría de administración de cuentas y módulos de negocio. No hay registro público ni recuperación simulada. Los cambios siguen siendo locales hasta publicarlos expresamente.
