package footballmarket.services.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que el mercado todavía no fue inicializado. */
public class MarketNotInitializedException extends DomainException {
  public MarketNotInitializedException() {
    super("El mercado todavía no fue inicializado");
  }
}
