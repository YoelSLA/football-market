package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Excepción lanzada cuando los filtros de rango de precio son negativos o invertidos. */
public class InvalidPriceRangeException extends ApplicationException {
  public InvalidPriceRangeException(String message) {
    super(message);
  }

  public InvalidPriceRangeException() {
    super("El rango de precios solicitado no es válido");
  }
}
