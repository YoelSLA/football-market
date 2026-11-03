package footballmarket.features.quote.application.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que el jugador no tiene una cotización disponible. */
public class QuoteNotFoundException extends DomainException {
  public QuoteNotFoundException() {
    super("No hay cotización disponible para el jugador");
  }
}
