package footballmarket.integrations.thesportsdb.exceptions;

import footballmarket.common.exceptions.IntegrationException;

/** Error transitorio de comunicación con TheSportsDB. */
public class TheSportsDbUnavailableException extends IntegrationException {
  public TheSportsDbUnavailableException() {
    super("El proveedor de imágenes no está disponible");
  }
}
