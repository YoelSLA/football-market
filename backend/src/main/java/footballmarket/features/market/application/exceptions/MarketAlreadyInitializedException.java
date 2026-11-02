package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que la inicialización solicitada no procede porque el mercado ya está inicializado. */
public class MarketAlreadyInitializedException extends DomainException {
  public MarketAlreadyInitializedException() {
    super("El mercado ya está inicializado en otra fecha de mercado");
  }
}
