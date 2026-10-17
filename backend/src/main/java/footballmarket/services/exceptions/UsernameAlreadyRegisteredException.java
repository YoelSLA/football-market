package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

/** Indica que el nombre de usuario solicitado ya está en uso por otro usuario. */
public class UsernameAlreadyRegisteredException extends ApplicationException {
  public UsernameAlreadyRegisteredException(String message) {
    super(message);
  }
}
