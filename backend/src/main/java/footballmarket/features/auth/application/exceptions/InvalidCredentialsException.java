package footballmarket.features.auth.application.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que las credenciales proporcionadas no permiten autenticar al usuario. */
public class InvalidCredentialsException extends DomainException {

  public InvalidCredentialsException(String message) {
    super(message);
  }
}
