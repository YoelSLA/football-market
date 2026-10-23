package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

public class PlayerStatisticsSyncInProgressException extends ApplicationException {
  public PlayerStatisticsSyncInProgressException() {
    super("Ya hay una sincronización de estadísticas en curso para el canal");
  }
}
