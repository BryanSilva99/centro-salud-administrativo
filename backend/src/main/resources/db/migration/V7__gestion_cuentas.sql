-- La misma versión detecta conflictos entre edición de cuentas y asignación de roles.
CREATE FUNCTION incrementar_version_edicion_cuenta() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.identificador IS DISTINCT FROM OLD.identificador OR NEW.activa IS DISTINCT FROM OLD.activa THEN
        NEW.version_roles := OLD.version_roles + 1;
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER cuentas_version_edicion BEFORE UPDATE ON cuentas
FOR EACH ROW EXECUTE FUNCTION incrementar_version_edicion_cuenta();

CREATE TABLE auditoria_cuentas (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    cuenta_id UUID NOT NULL,
    tipo VARCHAR(16) NOT NULL CHECK(tipo IN ('CREACION','ACTUALIZACION')),
    identificador_anterior TEXT,
    identificador_nuevo TEXT NOT NULL,
    activa_anterior BOOLEAN,
    activa_nueva BOOLEAN NOT NULL,
    roles VARCHAR(3)[] NOT NULL,
    fecha TIMESTAMPTZ NOT NULL
);
CREATE INDEX auditoria_cuentas_cuenta_fecha ON auditoria_cuentas(cuenta_id,fecha DESC,id DESC);
