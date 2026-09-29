package footballmarket.models.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que la contraseña requerida está vacía. */
public class EmptyPasswordException extends DomainException {
  public EmptyPasswordException(String message) {
    super(message);
  }
}
