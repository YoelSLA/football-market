package footballmarket.features.auth.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Indica que el nombre de usuario solicitado ya está en uso por otro usuario. */
public class UsernameAlreadyRegisteredException extends ApplicationException {
  public UsernameAlreadyRegisteredException(String message) {
    super(message);
  }
}
