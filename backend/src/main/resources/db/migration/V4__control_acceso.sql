ALTER TABLE cuentas ADD COLUMN version_acceso BIGINT NOT NULL DEFAULT 0;

-- Un cambio de credencial o estado revoca incluso sesiones que aún no se han usado.
CREATE FUNCTION incrementar_version_acceso() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.activa IS DISTINCT FROM OLD.activa OR NEW.contrasena_hash IS DISTINCT FROM OLD.contrasena_hash THEN
        NEW.version_acceso := OLD.version_acceso + 1;
    ELSE
        NEW.version_acceso := OLD.version_acceso;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER cuentas_version_acceso BEFORE UPDATE ON cuentas
FOR EACH ROW EXECUTE FUNCTION incrementar_version_acceso();

CREATE TABLE intentos_login (
    clave VARCHAR(64) PRIMARY KEY,
    fallos BIGINT[] NOT NULL DEFAULT '{}',
    bloqueado_hasta BIGINT,
    actualizado BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT intentos_login_max_fallos CHECK (cardinality(fallos) < 5)
);
CREATE INDEX intentos_login_actualizado ON intentos_login (actualizado);
