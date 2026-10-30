package footballmarket.features.identity.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** El origen no cumple las condiciones de un pase complementario. */
public class PlayerIdentitySourceNotEligibleException extends ApplicationException {
  public PlayerIdentitySourceNotEligibleException() {
    super("La ejecución de origen no es elegible");
  }
}
