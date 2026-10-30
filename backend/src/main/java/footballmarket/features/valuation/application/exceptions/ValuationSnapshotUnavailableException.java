package footballmarket.features.valuation.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Ausencia del snapshot poblacional confirmado requerido para evaluar. */
public class ValuationSnapshotUnavailableException extends ApplicationException {
  public ValuationSnapshotUnavailableException() {
    super("No existe un snapshot poblacional confirmado para evaluar");
  }
}
