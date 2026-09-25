package footballmarket.services.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que las credenciales proporcionadas no permiten autenticar al usuario. */
public class InvalidCredentialsException extends DomainException {

  public InvalidCredentialsException(String message) {
    super(message);
  }
}
