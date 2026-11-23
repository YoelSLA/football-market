package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Indica que la configuración de comisión de la venta oficial está fuera del rango admitido. */
public class InvalidOfficialBuybackConfigurationException extends ApplicationException {

  /**
   * Crea el rechazo con un motivo legible.
   *
   * @param message motivo del rechazo
   */
  public InvalidOfficialBuybackConfigurationException(String message) {
    super(message);
  }
}
