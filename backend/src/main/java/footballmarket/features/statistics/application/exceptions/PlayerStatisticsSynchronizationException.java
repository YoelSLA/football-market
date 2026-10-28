package footballmarket.features.statistics.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Fallo controlado de configuración o coordinación de estadísticas. */
public class PlayerStatisticsSynchronizationException extends ApplicationException {
  public PlayerStatisticsSynchronizationException() {
    super("No se pudo completar la sincronización de estadísticas WhoScored");
  }

  public PlayerStatisticsSynchronizationException(String message) {
    super(message);
  }
}
