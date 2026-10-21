package footballmarket.models.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que el estado persistido no permite reconstruir el portfolio de forma consistente. */
public class PortfolioIntegrityException extends DomainException {
  public PortfolioIntegrityException() {
    super("El estado del mercado no permite reconstruir el portfolio de forma consistente.");
  }
}
