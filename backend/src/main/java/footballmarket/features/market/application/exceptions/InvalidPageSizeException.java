package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/**
 * Excepción lanzada cuando el tamaño de página solicitado está fuera del rango permitido (1–100).
 */
public class InvalidPageSizeException extends ApplicationException {
  public InvalidPageSizeException(String message) {
    super(message);
  }

  public InvalidPageSizeException() {
    super("El tamaño de página debe estar entre 1 y 100");
  }
}
