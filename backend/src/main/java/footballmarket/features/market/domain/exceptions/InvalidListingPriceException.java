package footballmarket.features.market.domain.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que el precio acordado no pertenece a la banda permitida por la cotización. */
public class InvalidListingPriceException extends DomainException {
  public InvalidListingPriceException() {
    super("El precio de publicación debe estar entre el 80% y el 120% de la cotización vigente.");
  }
}
