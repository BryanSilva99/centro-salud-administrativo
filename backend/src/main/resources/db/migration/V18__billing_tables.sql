-- Migración del módulo de Caja (billing)
-- RF-19 a RF-24, RF-27

CREATE TABLE cargos (
    id UUID PRIMARY KEY,
    cita_id UUID NOT NULL,
    paciente_id UUID NOT NULL,
    importe NUMERIC(12, 2) NOT NULL CHECK (importe >= 0),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'PARCIAL', 'PAGADO', 'ANULADO'))
);

CREATE TABLE sesiones_caja (
    id UUID PRIMARY KEY,
    cajero_id UUID NOT NULL,
    fondo_inicial NUMERIC(12, 2) NOT NULL CHECK (fondo_inicial >= 0),
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('ABIERTA', 'CERRADA')),
    fecha_apertura TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_cierre TIMESTAMP WITH TIME ZONE,
    efectivo_declarado NUMERIC(12, 2),
    diferencia NUMERIC(12, 2),
    motivo_diferencia VARCHAR(500)
);

CREATE TABLE pagos (
    id UUID PRIMARY KEY,
    cargo_id UUID NOT NULL REFERENCES cargos(id),
    sesion_caja_id UUID NOT NULL REFERENCES sesiones_caja(id),
    importe NUMERIC(12, 2) NOT NULL CHECK (importe > 0),
    medio VARCHAR(20) NOT NULL CHECK (medio IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'OTRO')),
    referencia VARCHAR(100),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('COBRO', 'DEVOLUCION')),
    fecha_registro TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE solicitudes_reversion (
    id UUID PRIMARY KEY,
    operacion_id UUID NOT NULL,
    operacion_tipo VARCHAR(20) NOT NULL CHECK (operacion_tipo IN ('CARGO', 'PAGO')),
    solicitante_id UUID NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')),
    aprobador_id UUID,
    fecha_solicitud TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_resolucion TIMESTAMP WITH TIME ZONE
);

-- Índices para consultas frecuentes
CREATE INDEX idx_cargos_cita ON cargos(cita_id);
CREATE INDEX idx_cargos_paciente ON cargos(paciente_id);
CREATE INDEX idx_pagos_cargo ON pagos(cargo_id);
CREATE INDEX idx_pagos_sesion ON pagos(sesion_caja_id);
CREATE INDEX idx_pagos_fecha ON pagos(fecha_registro);
CREATE INDEX idx_sesiones_cajero_estado ON sesiones_caja(cajero_id, estado);
CREATE INDEX idx_solicitudes_solicitante ON solicitudes_reversion(solicitante_id);

CREATE UNIQUE INDEX cargo_vigente_cita ON cargos(cita_id) WHERE estado <> 'ANULADO';
CREATE UNIQUE INDEX sesion_abierta_cajero ON sesiones_caja(cajero_id) WHERE estado = 'ABIERTA';
CREATE TABLE control_caja (id INTEGER PRIMARY KEY CHECK (id=1));
INSERT INTO control_caja VALUES (1);
CREATE TABLE reintentos_caja (clave UUID PRIMARY KEY, actor UUID NOT NULL, contenido TEXT NOT NULL, resultado UUID NOT NULL);
CREATE TABLE compensaciones_caja (
 original UUID NOT NULL, tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('CARGO','PAGO')),
 solicitud UUID NOT NULL UNIQUE REFERENCES solicitudes_reversion(id),
 movimiento UUID REFERENCES pagos(id), PRIMARY KEY(original,tipo)
);
