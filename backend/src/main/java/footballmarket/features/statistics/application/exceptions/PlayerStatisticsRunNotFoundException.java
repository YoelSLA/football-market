package footballmarket.features.statistics.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

public class PlayerStatisticsRunNotFoundException extends ApplicationException {
  public PlayerStatisticsRunNotFoundException() {
    super("No se encontró la ejecución de estadísticas");
  }
}
