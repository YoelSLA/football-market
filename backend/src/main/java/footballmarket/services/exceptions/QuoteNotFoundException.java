package footballmarket.services.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que el jugador no tiene una cotización disponible. */
public class QuoteNotFoundException extends DomainException {
  public QuoteNotFoundException() {
    super("No hay cotización disponible para el jugador");
  }
}
