package footballmarket.exceptions;

/** Categoría de errores controlados de casos de uso ajenos al dominio y a la persistencia. */
public abstract class ApplicationException extends FootballMarketException {
  protected ApplicationException(String message) {
    super(message);
  }
}
