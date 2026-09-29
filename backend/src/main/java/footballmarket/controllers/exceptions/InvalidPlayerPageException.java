package footballmarket.controllers.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que los parámetros de paginación del catálogo son inválidos. */
public class InvalidPlayerPageException extends DomainException {
  public InvalidPlayerPageException() {
    super("La página debe ser mayor o igual a 0 y el tamaño debe estar entre 1 y 100");
  }
}
