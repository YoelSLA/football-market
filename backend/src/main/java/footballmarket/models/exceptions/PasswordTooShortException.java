package footballmarket.models.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que la contraseña no alcanza la longitud mínima. */
public class PasswordTooShortException extends DomainException {
  public PasswordTooShortException(String message) {
    super(message);
  }
}
