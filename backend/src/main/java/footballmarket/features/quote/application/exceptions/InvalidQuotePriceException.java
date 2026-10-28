package footballmarket.features.quote.application.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que la cotización vigente disponible no tiene un precio utilizable. */
public class InvalidQuotePriceException extends DomainException {
  public InvalidQuotePriceException() {
    super("El precio vigente debe ser mayor que cero");
  }
}
