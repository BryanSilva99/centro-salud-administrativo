-- Reservas autónomas del módulo appointments. Las referencias son UUID hasta
-- que los módulos de pacientes y recursos publiquen sus contratos definitivos.
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE citas (
    id UUID PRIMARY KEY,
    paciente_id UUID NOT NULL,
    profesional_id UUID NOT NULL,
    servicio_id UUID NOT NULL,
    consultorio_id UUID NOT NULL,
    inicio TIMESTAMPTZ NOT NULL,
    fin TIMESTAMPTZ NOT NULL,
    estado VARCHAR(20) NOT NULL,
    duracion_minutos INTEGER NOT NULL,
    modalidad VARCHAR(40) NOT NULL,
    importe NUMERIC(12, 2) NOT NULL,
    creada_por UUID NOT NULL,
    creada_en TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT citas_intervalo_valido CHECK (fin > inicio),
    CONSTRAINT citas_duracion_positiva CHECK (duracion_minutos > 0),
    CONSTRAINT citas_importe_no_negativo CHECK (importe >= 0),
    CONSTRAINT citas_estado_valido CHECK (estado IN ('PROGRAMADA', 'EN_ESPERA', 'EN_ATENCION', 'ATENDIDA', 'CANCELADA', 'AUSENTE'))
);

CREATE INDEX citas_agenda_fecha ON citas (inicio);
CREATE INDEX citas_paciente ON citas (paciente_id);
CREATE INDEX citas_profesional ON citas (profesional_id);

-- Una cita cancelada o ausente libera todos sus recursos. [) permite que una
-- reserva empiece exactamente cuando termina otra.
ALTER TABLE citas ADD CONSTRAINT citas_no_solape_paciente
    EXCLUDE USING gist (paciente_id WITH =, tstzrange(inicio, fin, '[)') WITH &&)
    WHERE (estado NOT IN ('CANCELADA', 'AUSENTE'));
ALTER TABLE citas ADD CONSTRAINT citas_no_solape_profesional
    EXCLUDE USING gist (profesional_id WITH =, tstzrange(inicio, fin, '[)') WITH &&)
    WHERE (estado NOT IN ('CANCELADA', 'AUSENTE'));
ALTER TABLE citas ADD CONSTRAINT citas_no_solape_consultorio
    EXCLUDE USING gist (consultorio_id WITH =, tstzrange(inicio, fin, '[)') WITH &&)
    WHERE (estado NOT IN ('CANCELADA', 'AUSENTE'));

CREATE TABLE reprogramaciones_cita (
    id UUID PRIMARY KEY,
    cita_id UUID NOT NULL REFERENCES citas(id),
    actor_id UUID NOT NULL,
    horario_anterior_inicio TIMESTAMPTZ NOT NULL,
    horario_anterior_fin TIMESTAMPTZ NOT NULL,
    nuevo_horario_inicio TIMESTAMPTZ NOT NULL,
    nuevo_horario_fin TIMESTAMPTZ NOT NULL,
    motivo TEXT NOT NULL,
    registrada_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT reprogramacion_motivo_no_vacio CHECK (length(btrim(motivo)) > 0),
    CONSTRAINT reprogramacion_intervalos_validos CHECK (
        horario_anterior_fin > horario_anterior_inicio AND nuevo_horario_fin > nuevo_horario_inicio
    )
);
CREATE INDEX reprogramaciones_cita_idx ON reprogramaciones_cita (cita_id, registrada_en);
