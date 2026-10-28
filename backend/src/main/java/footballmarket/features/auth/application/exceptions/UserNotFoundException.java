package footballmarket.features.auth.application.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que no existe el usuario solicitado. */
public class UserNotFoundException extends DomainException {
  public UserNotFoundException(String message) {
    super(message);
  }
}
