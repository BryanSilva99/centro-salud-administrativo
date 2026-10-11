-- Sin EXCLUDE/GiST ni CREATE EXTENSION: la cuenta de la aplicación no es superusuaria y las
-- pruebas usan esquemas aislados que se eliminan al terminar (una extensión es global a la
-- base, no al esquema). La superposición se impide en la aplicación, bloqueando la fila del
-- recurso (profesional o consultorio) dentro de la misma transacción del alta.
CREATE TABLE turnos_profesional (
    id UUID PRIMARY KEY,
    profesional_id UUID NOT NULL REFERENCES profesionales(id),
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('DISPONIBLE','DESCANSO')),
    activo BOOLEAN NOT NULL DEFAULT true,
    creado_por UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    CHECK (hora_fin > hora_inicio)
);
CREATE INDEX turnos_profesional_recurso_fecha ON turnos_profesional(profesional_id, fecha) WHERE activo;

CREATE TABLE turnos_consultorio (
    id UUID PRIMARY KEY,
    consultorio_id UUID NOT NULL REFERENCES consultorios(id),
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('DISPONIBLE','DESCANSO')),
    activo BOOLEAN NOT NULL DEFAULT true,
    creado_por UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    CHECK (hora_fin > hora_inicio)
);
CREATE INDEX turnos_consultorio_recurso_fecha ON turnos_consultorio(consultorio_id, fecha) WHERE activo;

-- Alta/baja únicamente (sin edición de horas): un registro mínimo por evento basta.
CREATE TABLE auditoria_turnos (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    tipo_recurso VARCHAR(12) NOT NULL CHECK (tipo_recurso IN ('PROFESIONAL','CONSULTORIO')),
    turno_id UUID NOT NULL,
    tipo VARCHAR(16) NOT NULL CHECK (tipo IN ('CREACION','DESACTIVACION')),
    fecha TIMESTAMPTZ NOT NULL
);
CREATE INDEX auditoria_turnos_turno_fecha ON auditoria_turnos(turno_id, fecha DESC, id DESC);
