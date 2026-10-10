# Credenciales: búsqueda y verificación

Este paso implementa `VerificarCredenciales.ejecutar(identificador, contrasena)`. Si las credenciales coinciden y la cuenta está activa, devuelve una `Cuenta` sin secretos. En caso contrario lanza `CredencialesInvalidas` con el mismo mensaje genérico. No inicia sesión ni concede permisos.

## Recorrido

```text
VerificarCredenciales
  → ConsultarCredenciales → ConsultarCredencialesPostgres → CuentaJpaRepository
  → HashContrasenas → HashContrasenasSpring
  → comprobar estado activo
  → Cuenta o CredencialesInvalidas
```

## Qué aporta cada archivo

Rutas Java relativas a `backend/src/main/java/pe/centrosalud/`:

| Archivo | Motivo |
|---|---|
| `modules/identity/domain/PoliticaContrasenas.java` | Contiene las condiciones de nuevas contraseñas: de 12 a 128 puntos de código y no solo espacios. El mínimo viene de RNF-02; el máximo es una decisión del sistema. |
| `modules/identity/application/ports/ConsultarCredenciales.java` | Declara la búsqueda exacta por identificador sin depender de JPA. |
| `modules/identity/application/ports/CredencialesCuenta.java` | Transporta internamente cuenta y hash; su representación textual oculta el contenido. No es un DTO web ni un `record` que imprima automáticamente el hash. |
| `modules/identity/application/ports/HashContrasenas.java` | Contrato para generar y verificar hashes. Impide que el caso de uso dependa del algoritmo o de Spring. |
| `modules/identity/application/usecases/VerificarCredenciales.java` | Coordina búsqueda, comparación y estado. Nunca devuelve el hash ni incorpora los valores recibidos al error. |
| `modules/identity/application/usecases/CredencialesInvalidas.java` | Error único para no revelar existencia o estado de una cuenta. |
| `adapters/outbound/postgres/identity/ConsultarCredencialesPostgres.java` | Implementa la búsqueda mediante Spring Data. Devuelve datos internos, no una respuesta HTTP. |
| `adapters/outbound/security/HashContrasenasSpring.java` | Implementa PBKDF2 con Spring Security; sal aleatoria por generación y trabajo simulado cuando no hay hash válido. |
| `bootstrap/IdentityConfiguration.java` | Conecta ambos adaptadores con el caso de uso usando inyección por constructor. |

Se ampliaron `CuentaJpaEntity` y `CuentaJpaRepository` para cargar el hash y buscar por identificador. `Cuenta` y `CuentaResponse` siguen sin secretos.

## Persistencia y algoritmo

`backend/src/main/resources/db/migration/V3__credenciales_cuenta.sql` añade `contrasena_hash` con formato validado. Permite NULL para no inventar credenciales de cuentas anteriores; una cuenta sin hash no puede superar la verificación. La restricción comprueba el formato, no demuestra por sí sola cómo se generó el valor. La generación correcta corresponde al adaptador criptográfico.

PBKDF2-HMAC-SHA256: sal aleatoria de 16 bytes, 600 000 iteraciones, resultado de 256 bits. El prefijo `{pbkdf2-sha256-v1}` identifica la configuración. El hash no se descifra: Spring calcula y comprueba usando la contraseña recibida y la sal almacenada. Dos hashes de la misma contraseña deben ser diferentes por sus sales y ambos deben verificar correctamente.

La decisión y [referencia oficial](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/crypto/password/Pbkdf2PasswordEncoder.html) están documentadas en DA-03 del ADD. La implementación usa parámetros explícitos, no los predeterminados.

No se modifican mayúsculas ni espacios del identificador. La contraseña tampoco se recorta; cambiarla silenciosamente daría un resultado distinto del que escribió la persona.

## Pruebas y estado real

Desde `backend/`:

```bash
./mvnw test
RUN_POSTGRES_TESTS=true ./mvnw test
```

El segundo comando requiere la base local iniciada y `.env` configurado. Aplica V3 y crea cuentas y contraseñas aleatorias exclusivamente en transacciones de prueba que se revierten. No hay credencial compartida ni cuenta de demostración permanente.

Se verifican acierto, contraseña incorrecta, cuenta ausente, cuenta inactiva, cuenta sin hash, búsqueda exacta, sal aleatoria, hashes inválidos, política de entrada y rechazo de texto plano por la base. Las pruebas anteriores de consulta y seguridad también se mantienen.

Este paso está verificado sin abrir una ruta de login. La cadena HTTP mantiene `denyAll`. Faltan sesiones, limitación de intentos, roles, endpoints de autenticación y formulario React; RF-01 y RNF-02 no se consideran completos.

## Qué aplica a otros proyectos

Guardar hashes con sal, usar errores genéricos y no registrar secretos son prácticas generales de autenticación por contraseña. PBKDF2, sus parámetros, el máximo de 128 caracteres y la búsqueda sensible a mayúsculas son decisiones de este proyecto. La separación en puertos y adaptadores corresponde a la arquitectura hexagonal elegida.
