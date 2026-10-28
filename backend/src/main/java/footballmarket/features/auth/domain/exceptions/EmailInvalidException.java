package footballmarket.features.auth.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que el email no tiene un formato válido. */
public class EmailInvalidException extends DomainException {
  public EmailInvalidException(String message) {
    super(message);
  }
}
