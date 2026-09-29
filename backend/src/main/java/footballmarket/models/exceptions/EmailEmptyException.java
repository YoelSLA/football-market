package footballmarket.models.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que el email requerido está vacío. */
public class EmailEmptyException extends DomainException {
  public EmailEmptyException(String message) {
    super(message);
  }
}
