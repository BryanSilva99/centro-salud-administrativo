package pe.centrosalud.adapters.outbound.postgres.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import pe.centrosalud.modules.identity.domain.Cuenta;

@Entity
@Table(name = "cuentas")
public class CuentaJpaEntity {
  @Id private UUID id;

  @Column(nullable = false, unique = true, columnDefinition = "text")
  private String identificador;

  @Column(nullable = false)
  private boolean activa;

  @Column(name = "contrasena_hash", columnDefinition = "text")
  private String contrasenaHash;

  @Column(name = "version_acceso", nullable = false)
  private long versionAcceso;

  protected CuentaJpaEntity() {
    // JPA necesita un constructor sin argumentos para reconstruir la fila.
  }

  pe.centrosalud.modules.identity.application.ports.CredencialesCuenta toCredenciales() {
    return new pe.centrosalud.modules.identity.application.ports.CredencialesCuenta(
        toDomain(), contrasenaHash, versionAcceso);
  }

  Cuenta toDomain() {
    return new Cuenta(id, identificador, activa);
  }
}
