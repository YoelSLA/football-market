package footballmarket.services.exceptions;

import footballmarket.exceptions.ApplicationException;

public class PlayerStatisticsRunNotFoundException extends ApplicationException {
  public PlayerStatisticsRunNotFoundException() {
    super("No se encontró la ejecución de estadísticas");
  }
}
