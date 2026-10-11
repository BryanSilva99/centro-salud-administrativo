package pe.centrosalud.modules.identity.domain;

import java.util.EnumSet;
import java.util.Set;

/** Matriz de acceso: ADM no recibe funciones de recepción sin tener también el rol REC. */
public final class PermisosCuenta {
  private PermisosCuenta() {}

  public static Set<Permiso> para(Set<Rol> roles) {
    var permisos = EnumSet.noneOf(Permiso.class);
    if (roles.contains(Rol.ADM)) {
      permisos.addAll(
          Set.of(
              Permiso.CUENTAS_CONSULTAR,
              Permiso.CUENTAS_GESTIONAR,
              Permiso.ROLES_GESTIONAR,
              Permiso.SERVICIOS_GESTIONAR,
              Permiso.CONSULTORIOS_GESTIONAR,
              Permiso.PROFESIONALES_GESTIONAR,
              Permiso.DISPONIBILIDAD_GESTIONAR));
    }
    if (roles.contains(Rol.REC)) permisos.add(Permiso.PACIENTES_GESTIONAR);
    if (roles.contains(Rol.REC) || roles.contains(Rol.PRO) || roles.contains(Rol.ADM))
      permisos.add(Permiso.CITAS_CONSULTAR);
    return Set.copyOf(permisos);
  }
}
