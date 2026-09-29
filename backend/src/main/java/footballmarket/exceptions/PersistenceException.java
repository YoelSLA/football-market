package footballmarket.exceptions;

/** Excepción base para errores controlados de persistencia. */
public abstract class PersistenceException extends FootballMarketException {

  /**
   * Crea una excepción de persistencia con un mensaje descriptivo.
   *
   * @param message mensaje seguro que describe el error
   */
  protected PersistenceException(String message) {
    super(message);
  }
}
