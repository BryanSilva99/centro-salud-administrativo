-- La secuencia garantiza códigos únicos ante altas concurrentes; una transacción revertida deja un hueco.
CREATE SEQUENCE pacientes_codigo_seq;
CREATE TABLE pacientes (
    id UUID PRIMARY KEY,
    codigo VARCHAR(9) NOT NULL UNIQUE DEFAULT 'P' || lpad(nextval('pacientes_codigo_seq')::text, 8, '0'),
    tipo_documento VARCHAR(3) NOT NULL CHECK (tipo_documento IN ('DNI','CE')),
    numero_documento VARCHAR(9) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    telefono VARCHAR(20),
    correo VARCHAR(254),
    creado_por UUID NOT NULL,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp,
    -- RN-04: el documento identifica al paciente; el nombre no.
    CONSTRAINT pacientes_documento_unique UNIQUE (tipo_documento, numero_documento),
    CHECK ((tipo_documento = 'DNI' AND numero_documento ~ '^[0-9]{8}$')
        OR (tipo_documento = 'CE' AND numero_documento ~ '^[A-Z0-9]{9}$')),
    CHECK (btrim(nombres) <> '' AND btrim(apellidos) <> '')
);
ALTER SEQUENCE pacientes_codigo_seq OWNED BY pacientes.codigo;

-- Solo identificadores: la auditoría no copia datos personales del paciente.
CREATE TABLE auditoria_pacientes (
    id UUID PRIMARY KEY,
    actor_id UUID NOT NULL,
    paciente_id UUID NOT NULL REFERENCES pacientes(id),
    tipo VARCHAR(16) NOT NULL CHECK (tipo IN ('CREACION')),
    fecha TIMESTAMPTZ NOT NULL
);
CREATE INDEX auditoria_pacientes_paciente_fecha ON auditoria_pacientes(paciente_id, fecha DESC, id DESC);
