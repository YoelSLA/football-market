package footballmarket.features.portfolio.presentation.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que los parámetros de paginación del portfolio son inválidos. */
public class InvalidPortfolioPageException extends DomainException {
  public InvalidPortfolioPageException() {
    super("La página debe ser mayor o igual a 0 y el tamaño debe estar entre 1 y 100");
  }
}
