package pe.centrosalud.modules.billing.application.ports;

import java.util.*;
import java.util.function.Supplier;
import pe.centrosalud.modules.billing.domain.SolicitudReversion.TipoOperacion;

public interface RegistroCaja {
  <T> T ejecutar(Supplier<T> trabajo);

  Optional<Reintento> reintento(UUID clave);

  void registrar(UUID clave, UUID actor, String contenido, UUID resultado);

  boolean compensada(UUID original, TipoOperacion tipo);

  void compensar(UUID original, TipoOperacion tipo, UUID solicitud, UUID movimiento);

  record Reintento(UUID actor, String contenido, UUID resultado) {}
}
