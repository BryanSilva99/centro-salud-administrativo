ALTER TABLE citas
    ADD COLUMN cierre_tipo VARCHAR(20),
    ADD COLUMN cierre_motivo TEXT,
    ADD COLUMN cerrada_en TIMESTAMPTZ,
    ADD COLUMN cerrada_por UUID;

ALTER TABLE citas DROP CONSTRAINT citas_estado_marcas_consistentes;

ALTER TABLE citas
    ADD CONSTRAINT citas_cierre_tipo_valido CHECK (
        cierre_tipo IS NULL OR cierre_tipo IN ('CANCELACION', 'RETIRO_ESPERA', 'AUSENCIA')
    ),
    ADD CONSTRAINT citas_cierre_actor_consistente CHECK ((cerrada_en IS NULL) = (cerrada_por IS NULL)),
    ADD CONSTRAINT citas_cierre_consistente CHECK (
        (estado NOT IN ('CANCELADA', 'AUSENTE')
            AND cierre_tipo IS NULL AND cierre_motivo IS NULL AND cerrada_en IS NULL AND cerrada_por IS NULL)
        OR (estado = 'CANCELADA' AND cierre_tipo = 'CANCELACION'
            AND cierre_motivo IS NOT NULL AND length(btrim(cierre_motivo)) > 0
            AND cerrada_en IS NOT NULL AND cerrada_por IS NOT NULL
            AND llegada_en IS NULL AND llegada_por IS NULL
            AND atencion_iniciada_en IS NULL AND atencion_iniciada_por IS NULL
            AND atencion_finalizada_en IS NULL AND atencion_finalizada_por IS NULL)
        OR (estado = 'CANCELADA' AND cierre_tipo = 'RETIRO_ESPERA'
            AND cierre_motivo IS NOT NULL AND length(btrim(cierre_motivo)) > 0
            AND cerrada_en IS NOT NULL AND cerrada_por IS NOT NULL
            AND llegada_en IS NOT NULL AND llegada_por IS NOT NULL
            AND atencion_iniciada_en IS NULL AND atencion_iniciada_por IS NULL
            AND atencion_finalizada_en IS NULL AND atencion_finalizada_por IS NULL)
        OR (estado = 'AUSENTE' AND cierre_tipo = 'AUSENCIA'
            AND cierre_motivo IS NULL AND cerrada_en IS NOT NULL AND cerrada_por IS NOT NULL
            AND llegada_en IS NULL AND llegada_por IS NULL
            AND atencion_iniciada_en IS NULL AND atencion_iniciada_por IS NULL
            AND atencion_finalizada_en IS NULL AND atencion_finalizada_por IS NULL)
    ),
    ADD CONSTRAINT citas_estado_marcas_consistentes CHECK (
        (estado = 'PROGRAMADA' AND llegada_en IS NULL AND atencion_iniciada_en IS NULL AND atencion_finalizada_en IS NULL)
        OR (estado = 'EN_ESPERA' AND llegada_en IS NOT NULL AND atencion_iniciada_en IS NULL AND atencion_finalizada_en IS NULL)
        OR (estado = 'EN_ATENCION' AND llegada_en IS NOT NULL AND atencion_iniciada_en IS NOT NULL AND atencion_finalizada_en IS NULL)
        OR (estado = 'ATENDIDA' AND llegada_en IS NOT NULL AND atencion_iniciada_en IS NOT NULL AND atencion_finalizada_en IS NOT NULL)
        OR (estado IN ('CANCELADA', 'AUSENTE') AND cerrada_en IS NOT NULL AND cerrada_por IS NOT NULL)
    );
