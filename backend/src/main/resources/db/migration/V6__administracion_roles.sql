ALTER TABLE cuentas ADD COLUMN version_roles BIGINT NOT NULL DEFAULT 0;
CREATE TABLE control_administracion (id INTEGER PRIMARY KEY CHECK(id=1));
INSERT INTO control_administracion VALUES (1);
CREATE FUNCTION incrementar_version_roles() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        UPDATE cuentas SET version_roles=version_roles+1 WHERE id=OLD.cuenta_id;
        RETURN OLD;
    END IF;
    UPDATE cuentas SET version_roles=version_roles+1 WHERE id=NEW.cuenta_id;
    RETURN NEW;
END;
$$;
CREATE TRIGGER cuenta_roles_version AFTER INSERT OR DELETE ON cuenta_roles
FOR EACH ROW EXECUTE FUNCTION incrementar_version_roles();
CREATE TABLE auditoria_roles (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    cuenta_id UUID NOT NULL,
    anteriores VARCHAR(3)[] NOT NULL,
    nuevos VARCHAR(3)[] NOT NULL,
    fecha TIMESTAMPTZ NOT NULL
);
CREATE INDEX auditoria_roles_cuenta_fecha ON auditoria_roles(cuenta_id,fecha DESC,id DESC);
