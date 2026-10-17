ALTER TABLE citas
    ADD COLUMN llegada_en TIMESTAMPTZ,
    ADD COLUMN llegada_por UUID,
    ADD COLUMN atencion_iniciada_en TIMESTAMPTZ,
    ADD COLUMN atencion_iniciada_por UUID,
    ADD COLUMN atencion_finalizada_en TIMESTAMPTZ,
    ADD COLUMN atencion_finalizada_por UUID;

ALTER TABLE citas
    ADD CONSTRAINT citas_llegada_actor_consistente CHECK ((llegada_en IS NULL) = (llegada_por IS NULL)),
    ADD CONSTRAINT citas_inicio_actor_consistente CHECK ((atencion_iniciada_en IS NULL) = (atencion_iniciada_por IS NULL)),
    ADD CONSTRAINT citas_fin_actor_consistente CHECK ((atencion_finalizada_en IS NULL) = (atencion_finalizada_por IS NULL)),
    ADD CONSTRAINT citas_atencion_en_orden CHECK (
        (atencion_iniciada_en IS NULL OR llegada_en IS NOT NULL)
        AND (atencion_finalizada_en IS NULL OR atencion_iniciada_en IS NOT NULL)
        AND (atencion_iniciada_en IS NULL OR atencion_iniciada_en >= llegada_en)
        AND (atencion_finalizada_en IS NULL OR atencion_finalizada_en >= atencion_iniciada_en)
    ),
    ADD CONSTRAINT citas_estado_marcas_consistentes CHECK (
        estado IN ('CANCELADA', 'AUSENTE')
        OR (estado = 'PROGRAMADA' AND llegada_en IS NULL AND atencion_iniciada_en IS NULL AND atencion_finalizada_en IS NULL)
        OR (estado = 'EN_ESPERA' AND llegada_en IS NOT NULL AND atencion_iniciada_en IS NULL AND atencion_finalizada_en IS NULL)
        OR (estado = 'EN_ATENCION' AND llegada_en IS NOT NULL AND atencion_iniciada_en IS NOT NULL AND atencion_finalizada_en IS NULL)
        OR (estado = 'ATENDIDA' AND llegada_en IS NOT NULL AND atencion_iniciada_en IS NOT NULL AND atencion_finalizada_en IS NOT NULL)
    );

CREATE INDEX citas_espera_orden ON citas (inicio, llegada_en)
    WHERE estado IN ('EN_ESPERA', 'EN_ATENCION');
CREATE UNIQUE INDEX citas_atencion_profesional_activa ON citas (profesional_id)
    WHERE estado = 'EN_ATENCION';
CREATE UNIQUE INDEX citas_atencion_consultorio_activo ON citas (consultorio_id)
    WHERE estado = 'EN_ATENCION';
