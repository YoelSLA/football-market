package footballmarket.features.identity.presentation.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Identificador estructural de ejecución no positivo. */
public class InvalidPlayerIdentityIdException extends ApplicationException {
  public InvalidPlayerIdentityIdException() {
    super("El identificador de ejecución debe ser positivo");
  }
}
