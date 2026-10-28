package footballmarket.features.identity.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** La ejecución solicitada no está registrada. */
public class PlayerIdentityRunNotFoundException extends ApplicationException {
  public PlayerIdentityRunNotFoundException() {
    super("La ejecución de identidad no existe");
  }
}
