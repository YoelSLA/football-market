package footballmarket.exceptions;

/** Excepción base para errores controlados de dominio y reglas de aplicación. */
public abstract class DomainException extends FootballMarketException {

  /**
   * Crea una excepción de dominio con un mensaje descriptivo.
   *
   * @param message mensaje seguro que describe el error
   */
  protected DomainException(String message) {
    super(message);
  }
}
