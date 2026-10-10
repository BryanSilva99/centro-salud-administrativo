package pe.centrosalud.modules.identity.domain;

import java.util.Set;

public final class PermisosCuenta {
    private PermisosCuenta() {}
    public static Set<Permiso> para(Set<Rol> roles) {
        return roles.contains(Rol.ADM) ? Set.of(Permiso.CUENTAS_CONSULTAR, Permiso.CUENTAS_GESTIONAR, Permiso.ROLES_GESTIONAR) : Set.of();
    }
}
