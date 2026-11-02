package footballmarket.integrations.footballdata.exceptions;

import footballmarket.common.exceptions.IntegrationException;

/** Indica que la configuración requerida para Football-Data es inválida. */
public class InvalidFootballDataConfigurationException extends IntegrationException {

  /**
   * Crea el error con la validación de configuración incumplida.
   *
   * @param message mensaje que describe la configuración inválida
   */
  public InvalidFootballDataConfigurationException(String message) {
    super(message);
  }
}
