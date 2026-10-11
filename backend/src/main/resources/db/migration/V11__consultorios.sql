CREATE TABLE consultorios (
    id UUID PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0,
    creado_por UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    CHECK (btrim(codigo) <> '' AND btrim(nombre) <> '')
);
CREATE UNIQUE INDEX consultorios_codigo_unique ON consultorios (lower(codigo));

-- Sin sub-entidad propia: se reemplaza por completo en cada edición, como los roles de una cuenta.
CREATE TABLE consultorio_servicios (
    consultorio_id UUID NOT NULL REFERENCES consultorios(id),
    servicio_id UUID NOT NULL REFERENCES servicios(id),
    PRIMARY KEY (consultorio_id, servicio_id)
);

-- El catálogo no contiene datos personales: se conserva la foto completa antes y después del cambio.
CREATE TABLE auditoria_consultorios (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    consultorio_id UUID NOT NULL REFERENCES consultorios(id),
    tipo VARCHAR(16) NOT NULL CHECK (tipo IN ('CREACION','ACTUALIZACION')),
    anterior JSONB,
    nuevo JSONB NOT NULL,
    fecha TIMESTAMPTZ NOT NULL
);
CREATE INDEX auditoria_consultorios_consultorio_fecha ON auditoria_consultorios(consultorio_id, fecha DESC, id DESC);
