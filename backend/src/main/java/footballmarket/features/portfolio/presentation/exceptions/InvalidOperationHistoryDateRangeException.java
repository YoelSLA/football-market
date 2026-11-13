package footballmarket.features.portfolio.presentation.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Rango temporal vacío o invertido para el historial. */
public class InvalidOperationHistoryDateRangeException extends DomainException {
  public InvalidOperationHistoryDateRangeException() {
    super("La fecha inicial debe ser anterior a la fecha final.");
  }
}
