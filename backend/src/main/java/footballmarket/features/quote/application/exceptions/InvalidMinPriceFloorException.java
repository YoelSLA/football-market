package footballmarket.features.quote.application.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que el piso mínimo de precio no es positivo. */
public class InvalidMinPriceFloorException extends DomainException {
  public InvalidMinPriceFloorException() {
    super("El piso mínimo de precio debe ser mayor que cero");
  }
}
