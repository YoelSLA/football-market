package footballmarket.features.quote.application.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que el tope de variación está fuera del rango permitido. */
public class InvalidVariationCapException extends DomainException {
  public InvalidVariationCapException() {
    super("El tope de variación debe estar en el rango (0; 1]");
  }
}
