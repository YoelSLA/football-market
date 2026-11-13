package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Excepción lanzada cuando el parámetro de paginación es menor que cero. */
public class InvalidPaginationException extends ApplicationException {
  public InvalidPaginationException(String message) {
    super(message);
  }

  public InvalidPaginationException() {
    super("El número de página no puede ser negativo");
  }
}
