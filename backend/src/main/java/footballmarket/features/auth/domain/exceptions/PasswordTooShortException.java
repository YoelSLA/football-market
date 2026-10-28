package footballmarket.features.auth.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que la contraseña no alcanza la longitud mínima. */
public class PasswordTooShortException extends DomainException {
  public PasswordTooShortException(String message) {
    super(message);
  }
}
