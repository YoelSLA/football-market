package footballmarket.features.statistics.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

public class PlayerStatisticsSyncInProgressException extends ApplicationException {
  public PlayerStatisticsSyncInProgressException() {
    super("Ya hay una sincronización de estadísticas en curso para el canal");
  }
}
