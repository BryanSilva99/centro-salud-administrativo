package pe.centrosalud.modules.identity.application.usecases;

import static pe.centrosalud.modules.identity.application.roles.ErrorRoles.Codigo.*;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import pe.centrosalud.modules.administration.application.ports.FichasProfesionales;
import pe.centrosalud.modules.identity.application.ports.HashContrasenas;
import pe.centrosalud.modules.identity.application.ports.UnidadAcceso;
import pe.centrosalud.modules.identity.application.roles.CuentaRoles;
import pe.centrosalud.modules.identity.application.roles.ErrorRoles;
import pe.centrosalud.modules.identity.domain.PoliticaContrasenas;
import pe.centrosalud.modules.identity.domain.Rol;

/** Administración de identidad; no permite editar contraseñas ni eliminar cuentas. */
public final class GestionarCuentas {
  private final UnidadAcceso unidad;
  private final HashContrasenas hashes;
  private final FichasProfesionales fichas;

  public GestionarCuentas(UnidadAcceso unidad, HashContrasenas hashes, FichasProfesionales fichas) {
    this.unidad = unidad;
    this.hashes = hashes;
    this.fichas = fichas;
  }

  public CuentaRoles crear(UUID actor, String identificador, String contrasena, Set<Rol> roles) {
    String nombre = validarIdentificador(identificador);
    PoliticaContrasenas.validarNueva(contrasena);
    Set<Rol> asignados = Set.copyOf(roles);
    // El cálculo costoso se realiza antes del bloqueo. Solo el hash llega a persistencia.
    String hash = hashes.generar(contrasena);
    return unidad.ejecutar(
        op -> {
          autorizar(op, actor);
          return op.crear(actor, nombre, hash, asignados);
        });
  }

  public CuentaRoles actualizar(
      UUID actor, UUID id, String identificador, boolean activa, long version) {
    String nombre = validarIdentificador(identificador);
    if (version < 0) throw new IllegalArgumentException();
    return unidad.ejecutar(
        op -> {
          autorizar(op, actor);
          CuentaRoles anterior = op.cargar(id);
          if (anterior.version() != version) throw new ErrorRoles(CONFLICTO);
          if (anterior.activa() == activa && anterior.identificador().equals(nombre))
            return anterior;
          if (anterior.activa()
              && !activa
              && anterior.roles().contains(Rol.ADM)
              && !op.existeOtroAdministradorActivo(id)) throw new ErrorRoles(ULTIMO_ADMIN);
          return op.actualizar(actor, anterior, nombre, activa);
        });
  }

  public CuentaRoles vincular(UUID actor, UUID cuentaId, UUID profesionalId, long version) {
    if (version < 0) throw new IllegalArgumentException();
    return unidad.ejecutar(
        operacion -> {
          autorizar(operacion, actor);
          CuentaRoles cuenta = operacion.cargar(cuentaId);
          if (cuenta.version() != version) throw new ErrorRoles(CONFLICTO);
          if (profesionalId != null) {
            if (!cuenta.roles().contains(Rol.PRO)) throw new ErrorRoles(REQUIERE_PRO);
            if (fichas.buscarPorId(profesionalId).filter(ficha -> ficha.activa()).isEmpty()) {
              throw new ErrorRoles(PROFESIONAL_INVALIDO);
            }
            if (operacion.profesionalOcupado(profesionalId, cuentaId))
              throw new ErrorRoles(VINCULO_OCUPADO);
          }
          if (Objects.equals(cuenta.profesionalId(), profesionalId)) return cuenta;
          return operacion.vincular(actor, cuenta, profesionalId);
        });
  }

  private static void autorizar(UnidadAcceso.Operacion op, UUID actor) {
    CuentaRoles cuenta = op.cargar(actor);
    if (!cuenta.activa() || !cuenta.roles().contains(Rol.ADM)) throw new ErrorRoles(PROHIBIDO);
  }

  private static String validarIdentificador(String valor) {
    if (valor == null) throw new IllegalArgumentException();
    String nombre = valor.strip();
    if (!nombre.matches("[\\p{L}\\p{N}._@-]{1,254}")) throw new IllegalArgumentException();
    return nombre;
  }
}
