-- Primera representación de cuentas. Credenciales, roles y vínculos se añadirán
-- en migraciones posteriores al implementar sus casos de uso.
CREATE TABLE cuentas (
    id UUID PRIMARY KEY,
    identificador TEXT NOT NULL,
    activa BOOLEAN NOT NULL,
    CONSTRAINT cuentas_identificador_unique UNIQUE (identificador),
    CONSTRAINT cuentas_identificador_no_vacio CHECK (identificador ~ '[^[:space:]]')
);
