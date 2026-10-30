package footballmarket.features.auth.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que el email requerido está vacío. */
public class EmailEmptyException extends DomainException {
  public EmailEmptyException(String message) {
    super(message);
  }
}
