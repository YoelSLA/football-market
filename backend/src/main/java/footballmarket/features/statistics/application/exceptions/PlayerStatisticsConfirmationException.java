package footballmarket.features.statistics.application.exceptions;

import footballmarket.common.exceptions.PersistenceException;

/** Fallo de confirmación saneado; distingue la campaña ya confirmada de otros errores. */
public class PlayerStatisticsConfirmationException extends PersistenceException {
  private final boolean alreadyCompleted;

  public PlayerStatisticsConfirmationException(boolean alreadyCompleted) {
    super("No se pudo confirmar atómicamente el resultado de estadísticas");
    this.alreadyCompleted = alreadyCompleted;
  }

  public boolean isAlreadyCompleted() {
    return this.alreadyCompleted;
  }
}
