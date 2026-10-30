package footballmarket.features.market.application.exceptions;

import footballmarket.common.exceptions.ApplicationException;

/** Indica que la configuración del aprovisionamiento del SUPERUSER es inválida o incompleta. */
public class InvalidSuperuserConfigurationException extends ApplicationException {

  /**
   * Crea el error con la validación de configuración incumplida.
   *
   * @param message mensaje que describe la configuración inválida
   */
  public InvalidSuperuserConfigurationException(String message) {
    super(message);
  }
}
