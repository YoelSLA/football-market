package footballmarket.controllers.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que el filtro de estado del portfolio no tiene un formato válido. */
public class InvalidPortfolioStatusException extends DomainException {
  public InvalidPortfolioStatusException() {
    super("El parámetro de consulta no tiene un formato válido");
  }
}
