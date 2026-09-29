package footballmarket.models.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que el email no tiene un formato válido. */
public class EmailInvalidException extends DomainException {
  public EmailInvalidException(String message) {
    super(message);
  }
}
