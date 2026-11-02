package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que la cantidad solicitada no es un entero dentro del rango admitido. */
public class InvalidQuantityException extends DomainException {
  public InvalidQuantityException() {
    super("La cantidad debe ser un entero entre 1 y 100");
  }
}
