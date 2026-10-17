package footballmarket.services.exceptions;

import footballmarket.exceptions.DomainException;

/** Indica que el jugador no tiene una cotización vigente disponible. */
public class QuoteUnavailableException extends DomainException {
  public QuoteUnavailableException() {
    super("No hay precio vigente para el jugador");
  }
}
