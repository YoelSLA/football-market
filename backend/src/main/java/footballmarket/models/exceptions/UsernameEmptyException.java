package footballmarket.models.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que el nombre de usuario no fue proporcionado. */
public class UsernameEmptyException extends DomainException {
  public UsernameEmptyException(String message) {
    super(message);
  }
}
