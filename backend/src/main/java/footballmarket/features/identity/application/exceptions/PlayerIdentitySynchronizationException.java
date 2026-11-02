package footballmarket.features.identity.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Fallo controlado de configuración o ejecución que no permite devolver un resumen coherente. */
public class PlayerIdentitySynchronizationException extends ApplicationException {
  public PlayerIdentitySynchronizationException() {
    super("No se pudo completar la sincronización de identidad WhoScored");
  }
}
