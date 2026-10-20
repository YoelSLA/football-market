package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

/** Ausencia del snapshot poblacional confirmado requerido para evaluar. */
public class ValuationSnapshotUnavailableException extends ApplicationException {
  public ValuationSnapshotUnavailableException() {
    super("No existe un snapshot poblacional confirmado para evaluar");
  }
}
