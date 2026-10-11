ALTER TABLE profesionales ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Sin sub-entidad propia: se reemplaza por completo en cada edición, como los roles de una cuenta.
CREATE TABLE profesional_servicios (
    profesional_id UUID NOT NULL REFERENCES profesionales(id),
    servicio_id UUID NOT NULL REFERENCES servicios(id),
    PRIMARY KEY (profesional_id, servicio_id)
);

-- A diferencia de servicios/consultorios, aquí la foto no incluye nombre ni identificación
-- (RF-28/CA-2, RN-17): solo estado, versión y los IDs de servicios habilitados.
CREATE TABLE auditoria_profesionales (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    profesional_id UUID NOT NULL REFERENCES profesionales(id),
    tipo VARCHAR(16) NOT NULL CHECK (tipo IN ('CREACION','ACTUALIZACION')),
    anterior JSONB,
    nuevo JSONB NOT NULL,
    fecha TIMESTAMPTZ NOT NULL
);
CREATE INDEX auditoria_profesionales_profesional_fecha ON auditoria_profesionales(profesional_id, fecha DESC, id DESC);
