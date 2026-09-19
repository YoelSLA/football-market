package footballmarket.services.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que no existe el usuario solicitado. */
public class UserNotFoundException extends DomainException {
  public UserNotFoundException(String message) {
    super(message);
  }
}
