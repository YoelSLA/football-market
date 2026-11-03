package footballmarket.features.quote.application.exceptions;

import footballmarket.common.exceptions.IntegrationException;

/** Indica que no se pudo obtener el lote de cotizaciones; no significa ausencia de precios. */
public class QuoteBatchUnavailableException extends IntegrationException {
  public QuoteBatchUnavailableException() {
    super("La fuente de cotizaciones no permite completar la lectura masiva.");
  }

  /**
   * Crea el fallo conservando la causa técnica original.
   *
   * @param cause fallo producido por la fuente de cotizaciones
   */
  public QuoteBatchUnavailableException(Throwable cause) {
    super("La fuente de cotizaciones no permite completar la lectura masiva.");
    initCause(cause);
  }
}
