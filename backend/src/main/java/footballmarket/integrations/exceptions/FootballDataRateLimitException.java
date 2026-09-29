package footballmarket.integrations.exceptions;

import footballmarket.exceptions.IntegrationException;

/**
 * Excepción interna utilizada para representar que Football-Data alcanzó su límite de peticiones.
 *
 * <p>Transporta el tiempo que debe esperarse antes de volver a realizar la petición.
 */
public class FootballDataRateLimitException extends IntegrationException {

  private final long retryAfterSeconds;

  /**
   * Crea una excepción de límite de peticiones.
   *
   * @param retryAfterSeconds segundos que deben esperarse antes del siguiente intento
   */
  public FootballDataRateLimitException(long retryAfterSeconds) {
    super(
        "Se alcanzó el límite de solicitudes permitido por Football-Data. "
            + "Se podrá reintentar en "
            + retryAfterSeconds
            + " segundos.");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  /**
   * Devuelve el tiempo de espera solicitado por el proveedor.
   *
   * @return segundos que deben esperarse
   */
  public long retryAfterSeconds() {
    return this.retryAfterSeconds;
  }
}
