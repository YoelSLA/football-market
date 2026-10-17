package footballmarket.services.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que la cotización vigente disponible no tiene un precio utilizable. */
public class InvalidQuotePriceException extends DomainException {
  public InvalidQuotePriceException() {
    super("El precio vigente debe ser mayor que cero");
  }
}
