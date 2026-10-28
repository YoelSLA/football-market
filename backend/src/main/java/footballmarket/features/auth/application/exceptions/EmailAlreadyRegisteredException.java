package footballmarket.features.auth.application.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que el email solicitado ya pertenece a otro usuario. */
public class EmailAlreadyRegisteredException extends DomainException {

  public EmailAlreadyRegisteredException(String message) {
    super(message);
  }
}
