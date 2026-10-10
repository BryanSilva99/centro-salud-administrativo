-- Las cuentas existentes permanecen sin credencial hasta asignarla expresamente.
-- Nunca se usa una contraseña compartida ni se actualiza una migración aplicada.
ALTER TABLE cuentas ADD COLUMN contrasena_hash TEXT;
ALTER TABLE cuentas ADD CONSTRAINT cuentas_hash_formato CHECK (
    contrasena_hash IS NULL OR
    contrasena_hash ~ '^\{pbkdf2-sha256-v1\}[0-9a-f]{96}$'
);
