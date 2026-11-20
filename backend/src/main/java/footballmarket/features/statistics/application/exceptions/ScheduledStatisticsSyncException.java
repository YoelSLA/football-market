package footballmarket.features.statistics.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Fallo global de una sincronización programada, sin texto arbitrario del proveedor. */
public class ScheduledStatisticsSyncException extends ApplicationException {
  /** Identidades seguras comunicadas por el recorrido o por su estado terminal. */
  public enum Reason {
    PROVIDER_UNAVAILABLE,
    HEADED_BROWSER_UNAVAILABLE,
    EXECUTION_INTERRUPTED,
    EXECUTION_FAILED,
    CLOSE_FAILED,
    FAILED,
    INTERRUPTED
  }

  private final Reason reason;

  public ScheduledStatisticsSyncException(Reason reason) {
    super("La sincronización programada no pudo completarse: " + reason.name());
    this.reason = reason;
  }

  public Reason getReason() {
    return this.reason;
  }
}
