package pe.centrosalud.unit;

import java.util.Set;
import org.junit.jupiter.api.Test;
import pe.centrosalud.modules.identity.domain.Permiso;
import pe.centrosalud.modules.identity.domain.PermisosCuenta;
import pe.centrosalud.modules.identity.domain.Rol;
import static org.junit.jupiter.api.Assertions.*;

class PermisosCuentaTest {
    @Test void soloRecepcionGestionaPacientes() {
        assertEquals(Set.of(Permiso.PACIENTES_GESTIONAR), PermisosCuenta.para(Set.of(Rol.REC)));
        assertFalse(PermisosCuenta.para(Set.of(Rol.ADM)).contains(Permiso.PACIENTES_GESTIONAR));
        assertEquals(Set.of(), PermisosCuenta.para(Set.of(Rol.CAJ, Rol.PRO)));
    }

    @Test void variosRolesSumanSusPermisos() {
        assertEquals(Set.of(Permiso.CUENTAS_CONSULTAR, Permiso.CUENTAS_GESTIONAR, Permiso.ROLES_GESTIONAR, Permiso.PACIENTES_GESTIONAR),
                PermisosCuenta.para(Set.of(Rol.ADM, Rol.REC)));
    }
}
