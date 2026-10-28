package footballmarket.features.auth.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que el nombre de usuario no fue proporcionado. */
public class UsernameEmptyException extends DomainException {
  public UsernameEmptyException(String message) {
    super(message);
  }
}
