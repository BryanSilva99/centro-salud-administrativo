CREATE TABLE profesionales (
    id UUID PRIMARY KEY,
    identificacion VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    funcion VARCHAR(100) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT true,
    creado_por UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    CHECK (btrim(identificacion) <> '' AND btrim(nombre) <> '' AND btrim(funcion) <> '')
);
-- Una cuenta tiene como máximo una ficha, y cada ficha una cuenta.
ALTER TABLE cuentas ADD COLUMN profesional_id UUID REFERENCES profesionales(id);
ALTER TABLE cuentas ADD CONSTRAINT cuentas_profesional_unique UNIQUE(profesional_id);
ALTER TABLE auditoria_cuentas ADD COLUMN profesional_anterior UUID;
ALTER TABLE auditoria_cuentas ADD COLUMN profesional_nuevo UUID;
ALTER TABLE auditoria_cuentas DROP CONSTRAINT auditoria_cuentas_tipo_check;
ALTER TABLE auditoria_cuentas ADD CONSTRAINT auditoria_cuentas_tipo_check CHECK(tipo IN ('CREACION','ACTUALIZACION','VINCULACION'));
CREATE OR REPLACE FUNCTION incrementar_version_edicion_cuenta() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.identificador IS DISTINCT FROM OLD.identificador OR NEW.activa IS DISTINCT FROM OLD.activa
       OR NEW.profesional_id IS DISTINCT FROM OLD.profesional_id THEN
        NEW.version_roles := OLD.version_roles + 1;
    END IF;
    RETURN NEW;
END;
$$;
