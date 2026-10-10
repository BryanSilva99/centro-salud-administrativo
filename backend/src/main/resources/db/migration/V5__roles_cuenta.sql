-- Sin asignaciones automáticas ni superadministradores implícitos.
CREATE TABLE cuenta_roles (
    cuenta_id UUID NOT NULL REFERENCES cuentas(id) ON DELETE CASCADE,
    rol VARCHAR(3) NOT NULL CHECK (rol IN ('REC', 'CAJ', 'ADM', 'PRO')),
    PRIMARY KEY (cuenta_id, rol)
);
