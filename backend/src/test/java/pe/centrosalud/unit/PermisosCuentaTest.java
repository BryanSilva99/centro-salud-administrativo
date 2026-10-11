package pe.centrosalud.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.identity.domain.Permiso;
import pe.centrosalud.modules.identity.domain.PermisosCuenta;
import pe.centrosalud.modules.identity.domain.Rol;

class PermisosCuentaTest {
  @Test
  void soloRecepcionGestionaPacientes() {
    assertEquals(Set.of(Permiso.PACIENTES_GESTIONAR), PermisosCuenta.para(Set.of(Rol.REC)));
    assertFalse(PermisosCuenta.para(Set.of(Rol.ADM)).contains(Permiso.PACIENTES_GESTIONAR));
    assertEquals(Set.of(), PermisosCuenta.para(Set.of(Rol.CAJ, Rol.PRO)));
  }

  @Test
  void soloAdministracionGestionaServicios() {
    assertTrue(PermisosCuenta.para(Set.of(Rol.ADM)).contains(Permiso.SERVICIOS_GESTIONAR));
    assertFalse(
        PermisosCuenta.para(Set.of(Rol.REC, Rol.CAJ, Rol.PRO))
            .contains(Permiso.SERVICIOS_GESTIONAR));
  }

  @Test
  void soloAdministracionGestionaConsultorios() {
    assertTrue(PermisosCuenta.para(Set.of(Rol.ADM)).contains(Permiso.CONSULTORIOS_GESTIONAR));
    assertFalse(
        PermisosCuenta.para(Set.of(Rol.REC, Rol.CAJ, Rol.PRO))
            .contains(Permiso.CONSULTORIOS_GESTIONAR));
  }

  @Test
  void soloAdministracionGestionaProfesionales() {
    assertTrue(PermisosCuenta.para(Set.of(Rol.ADM)).contains(Permiso.PROFESIONALES_GESTIONAR));
    assertFalse(
        PermisosCuenta.para(Set.of(Rol.REC, Rol.CAJ, Rol.PRO))
            .contains(Permiso.PROFESIONALES_GESTIONAR));
  }

  @Test
  void soloAdministracionGestionaDisponibilidad() {
    assertTrue(PermisosCuenta.para(Set.of(Rol.ADM)).contains(Permiso.DISPONIBILIDAD_GESTIONAR));
    assertFalse(
        PermisosCuenta.para(Set.of(Rol.REC, Rol.CAJ, Rol.PRO))
            .contains(Permiso.DISPONIBILIDAD_GESTIONAR));
  }

  @Test
  void variosRolesSumanSusPermisos() {
    assertEquals(
        Set.of(
            Permiso.CUENTAS_CONSULTAR,
            Permiso.CUENTAS_GESTIONAR,
            Permiso.ROLES_GESTIONAR,
            Permiso.SERVICIOS_GESTIONAR,
            Permiso.CONSULTORIOS_GESTIONAR,
            Permiso.PROFESIONALES_GESTIONAR,
            Permiso.DISPONIBILIDAD_GESTIONAR,
            Permiso.PACIENTES_GESTIONAR),
        PermisosCuenta.para(Set.of(Rol.ADM, Rol.REC)));
  }
}
