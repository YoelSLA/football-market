package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

/** El origen no cumple las condiciones de un pase complementario. */
public class PlayerIdentitySourceNotEligibleException extends ApplicationException {
  public PlayerIdentitySourceNotEligibleException() {
    super("La ejecución de origen no es elegible");
  }
}
