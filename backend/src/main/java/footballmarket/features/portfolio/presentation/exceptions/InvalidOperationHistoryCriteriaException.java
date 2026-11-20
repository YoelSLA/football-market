package footballmarket.features.portfolio.presentation.exceptions;

import footballmarket.common.exceptions.DomainException;

/** Criterios de paginación, tipo u orden inválidos para el historial. */
public class InvalidOperationHistoryCriteriaException extends DomainException {
  public InvalidOperationHistoryCriteriaException() {
    super("Los criterios del historial de transacciones no son válidos.");
  }
}
