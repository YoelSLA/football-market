package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/**
 * Excepción lanzada cuando el criterio de ordenamiento es inválido o se envían múltiples criterios.
 */
public class InvalidSortParameterException extends ApplicationException {
  public InvalidSortParameterException(String message) {
    super(message);
  }

  public InvalidSortParameterException() {
    super("El parámetro de ordenamiento no es válido o contiene múltiples criterios");
  }
}
