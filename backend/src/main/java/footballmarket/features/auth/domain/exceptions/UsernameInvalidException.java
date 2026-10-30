package footballmarket.features.auth.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que el nombre de usuario no cumple el formato admitido o su longitud. */
public class UsernameInvalidException extends DomainException {
  public UsernameInvalidException(String message) {
    super(message);
  }
}
