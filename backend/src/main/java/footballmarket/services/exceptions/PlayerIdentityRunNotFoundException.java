package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

/** La ejecución solicitada no está registrada. */
public class PlayerIdentityRunNotFoundException extends ApplicationException {
  public PlayerIdentityRunNotFoundException() {
    super("La ejecución de identidad no existe");
  }
}
