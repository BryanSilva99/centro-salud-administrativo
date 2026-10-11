ALTER TABLE pacientes ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- La auditoría sigue sin copiar datos personales (RN-17): solo se amplía el tipo permitido.
ALTER TABLE auditoria_pacientes DROP CONSTRAINT auditoria_pacientes_tipo_check;
ALTER TABLE auditoria_pacientes ADD CONSTRAINT auditoria_pacientes_tipo_check CHECK (tipo IN ('CREACION','ACTUALIZACION'));
