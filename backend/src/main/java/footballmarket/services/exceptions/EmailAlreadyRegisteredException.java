package footballmarket.services.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que el email solicitado ya pertenece a otro usuario. */
public class EmailAlreadyRegisteredException extends DomainException {

  public EmailAlreadyRegisteredException(String message) {
    super(message);
  }
}
