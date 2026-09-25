package footballmarket.integrations.exceptions;

import footballmarket.exceptions.IntegrationException;

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
