package footballmarket.features.auth.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que la contraseña requerida está vacía. */
public class EmptyPasswordException extends DomainException {
  public EmptyPasswordException(String message) {
    super(message);
  }
}
