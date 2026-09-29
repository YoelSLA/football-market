package footballmarket.exceptions;

/** Excepción base de todos los errores propios y controlados de la aplicación. */
public abstract class FootballMarketException extends RuntimeException {

  /**
   * Crea una excepción controlada con un mensaje descriptivo.
   *
   * @param message mensaje seguro que describe el error
   */
  protected FootballMarketException(String message) {
    super(message);
  }
}
