package footballmarket.features.quote.application.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que la estrategia solicitada no pertenece al catálogo vigente. */
public class UnknownQuoteStrategyException extends DomainException {
  public UnknownQuoteStrategyException() {
    super("La estrategia solicitada no pertenece al catálogo vigente");
  }
}
