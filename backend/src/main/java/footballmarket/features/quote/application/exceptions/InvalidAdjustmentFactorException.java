package footballmarket.features.quote.application.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Indica que el factor de ajuste está fuera del rango permitido. */
public class InvalidAdjustmentFactorException extends DomainException {
  public InvalidAdjustmentFactorException() {
    super("El factor de ajuste debe estar en el rango [0.10; 1.00]");
  }
}
