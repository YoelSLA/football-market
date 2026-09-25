package footballmarket.integrations.exceptions;

import footballmarket.exceptions.IntegrationException;

/** Indica que no fue posible obtener una foto completa desde Football-Data. */
public class FootballDataUnavailableException extends IntegrationException {
  public FootballDataUnavailableException() {
    super("No se pudo completar la lectura del proveedor de jugadores");
  }
}
