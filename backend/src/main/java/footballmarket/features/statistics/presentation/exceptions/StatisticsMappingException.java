package footballmarket.features.statistics.presentation.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Falta una representación requerida en la frontera de presentación. */
public class StatisticsMappingException extends ApplicationException {
  public StatisticsMappingException() {
    super("No se pudo transformar la representación de estadísticas");
  }
}
