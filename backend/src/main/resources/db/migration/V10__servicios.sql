CREATE TABLE servicios (
    id UUID PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL CHECK (btrim(nombre) <> ''),
    duracion_minutos INTEGER NOT NULL CHECK (duracion_minutos BETWEEN 1 AND 480),
    activo BOOLEAN NOT NULL DEFAULT true,
    -- Control optimista: cualquier cambio del servicio o de sus modalidades la incrementa.
    version BIGINT NOT NULL DEFAULT 0,
    creado_por UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp
);
CREATE UNIQUE INDEX servicios_nombre_unique ON servicios (lower(nombre));

-- Las modalidades no se eliminan: se desactivan para que las citas futuras puedan referenciarlas (RN-03).
CREATE TABLE servicio_modalidades (
    id UUID PRIMARY KEY,
    servicio_id UUID NOT NULL REFERENCES servicios(id),
    nombre VARCHAR(60) NOT NULL CHECK (btrim(nombre) <> ''),
    con_cargo BOOLEAN NOT NULL,
    tarifa NUMERIC(10,2) NOT NULL CHECK (tarifa >= 0),
    activa BOOLEAN NOT NULL DEFAULT true,
    -- S/ 0 solo existe como modalidad sin cargo (RF-19/CA-3).
    CHECK ((con_cargo AND tarifa > 0) OR (NOT con_cargo AND tarifa = 0))
);
CREATE UNIQUE INDEX servicio_modalidades_nombre_unique ON servicio_modalidades (servicio_id, lower(nombre));

-- El catálogo no contiene datos personales: se conserva la foto completa antes y después del cambio.
CREATE TABLE auditoria_servicios (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    servicio_id UUID NOT NULL REFERENCES servicios(id),
    tipo VARCHAR(16) NOT NULL CHECK (tipo IN ('CREACION','ACTUALIZACION')),
    anterior JSONB,
    nuevo JSONB NOT NULL,
    fecha TIMESTAMPTZ NOT NULL
);
CREATE INDEX auditoria_servicios_servicio_fecha ON auditoria_servicios(servicio_id, fecha DESC, id DESC);
