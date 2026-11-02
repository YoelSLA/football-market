package footballmarket.features.market.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que una cantidad de tokens debe ser un entero positivo. */
public class InvalidTokenQuantityException extends DomainException {
  public InvalidTokenQuantityException() {
    super("La cantidad de tokens debe ser un entero positivo.");
  }
}
