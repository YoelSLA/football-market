package footballmarket.integrations.exceptions;

import footballmarket.exceptions.IntegrationException;

/** Error transitorio de comunicación con TheSportsDB. */
public class TheSportsDbUnavailableException extends IntegrationException {
  public TheSportsDbUnavailableException() {
    super("El proveedor de imágenes no está disponible");
  }
}
