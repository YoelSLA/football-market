package footballmarket.exceptions;

/** Excepción base para errores controlados de integraciones externas. */
public abstract class IntegrationException extends FootballMarketException {

  /**
   * Crea una excepción de integración con un mensaje descriptivo.
   *
   * @param message mensaje seguro que describe el error
   */
  protected IntegrationException(String message) {
    super(message);
  }
}
